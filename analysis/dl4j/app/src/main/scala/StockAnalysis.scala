package org.analyzer

import java.io.{BufferedOutputStream, IOException}
import java.nio.channels.UnresolvedAddressException

import org.apache.spark.SparkConf
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.{BackpropType, NeuralNetConfiguration}
import org.deeplearning4j.nn.conf.layers.{LSTM, RnnOutputLayer}
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.spark.impl.multilayer.SparkDl4jMultiLayer
import org.deeplearning4j.spark.parameterserver.training.SharedTrainingMaster
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Nadam
import org.nd4j.parameterserver.distributed.conf.VoidConfiguration
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.hadoop.io.IOUtils
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader
import org.datavec.spark.functions.SequenceRecordReaderFunction
import org.deeplearning4j.optimize.solvers.accumulation.encoding.residual.ResidualClippingPostProcessor
import org.deeplearning4j.optimize.solvers.accumulation.encoding.threshold.AdaptiveThresholdAlgorithm
import org.deeplearning4j.spark.api.RDDTrainingApproach
import org.deeplearning4j.spark.datavec.DataVecSequenceDataSetFunction
import org.deeplearning4j.util.ModelSerializer
import org.mlflow.tracking.MlflowClient
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction

import scala.util.Try

object StockAnalysis {

  val stockFolder = "hdfs://namenode:9000/topics/stock_test/partition=0"

  def copyMerge(
    fs: FileSystem, srcDir: Path,
    dstFile: Path,
    deleteSource: Boolean, conf: Configuration): Boolean = {

    if (fs.exists(dstFile))
      throw new IOException(s"Target $dstFile already exists")

    // Source path is expected to be a directory:
    if (fs.getFileStatus(srcDir).isDirectory()) {

      val outputFile = fs.create(dstFile)
      Try {
        fs
          .listStatus(srcDir)
          .sortBy(_.getPath.getName)
          .collect {
            case status if status.isFile() =>
              val inputFile = fs.open(status.getPath())
              Try(IOUtils.copyBytes(inputFile, outputFile, conf, false))
              inputFile.close()
          }
      }
      outputFile.close()

      if (deleteSource) fs.delete(srcDir, true) else true
    } else false
  }

  def main(args: Array[String]): Unit = {

    val sparkConf = new SparkConf()
    sparkConf.setMaster("spark://spark-master:7077")
    sparkConf.setAppName("Stock Analysis")
    sparkConf.set("spark.hadoop.fs.defaultFS", "hdfs://namenode:9000")
    val spark = SparkSession.builder.config("spark.hadoop.dfs.client.use.datanode.hostname", "true").config("spark.hadoop.dfs.datanode.https.address", "datanode:9864").appName("Stock Analysis").getOrCreate()
    try {
      // val stockData = spark.read.json(hdfsMaster + "/topics/stock_test/partition=0/stock_test+0+0000000000+0000000000.json")
      /**
       * root
       * |-- change: double (nullable = true)
       * |-- changeOverTime: double (nullable = true)
       * |-- changePercent: double (nullable = true)
       * |-- close: double (nullable = true)
       * |-- date: string (nullable = true)
       * |-- high: double (nullable = true)
       * |-- label: string (nullable = true)
       * |-- low: double (nullable = true)
       * |-- open: double (nullable = true)
       * |-- symbol: string (nullable = true)
       * |-- unadjustedVolume: long (nullable = true)
       * |-- volume: long (nullable = true)
       * |-- vwap: double (nullable = true)
       */
      val LOOK_AHEAD = 5
      val NUM_EPOCHS = 2
      val LEARNING_RATE = 0.002
      val BATCH_SIZE = 70
      val NUM_NEURONS = 10
      val FEATURE_SIZE = 5
      val DROPOUT_RATE = 0.7
      val NUM_CLASSES = 2
      val LABEL_INDEX = 5

      // PREPROCESS STEP
      import spark.implicits._

      val hadoopConfig = new Configuration()
      hadoopConfig.set("fs.defaultFS", "hdfs://namenode:9000")
      val hdfs = FileSystem.get(hadoopConfig)

      def createDir(outputfile: String): Unit = {
        if (!hdfs.exists(new Path(outputfile))) {
          hdfs.mkdirs(new Path(outputfile))
        } else {
          hdfs.delete(new Path(outputfile), true)
          hdfs.mkdirs(new Path(outputfile))
        }
      }

      def preprocessData(stockLocation: String, outputdir: String, i: Int): Unit = {
        val loadStock = spark.read.json(stockLocation)
        val window = Window.orderBy("date")
        val indexAppended = loadStock.withColumn("index", row_number().over(window))
        val selfJoined = indexAppended.as("d1")
          .join(
            indexAppended.as("d2")
              .select("close", "index")
              .withColumnRenamed("close", "future"),
            $"d1.index" === $"d2.index" + 5)
          .select($"d1.close".as("close"), $"open", $"high", $"low", $"volume", $"future")
          .withColumn("open", when($"open".isNull, 0.0).otherwise($"open"))
          .withColumn("high", when($"high".isNull, 0.0).otherwise($"high"))
          .withColumn("low", when($"low".isNull, 0.0).otherwise($"low"))

        if (selfJoined.count() == 0) return

        val trainCSV: Dataset[(Double, Double, Double, Double, Long, Int)] = selfJoined.map({
          case Row(close: Double, open: Double, high: Double, low: Double, volume: Long, future: Double) =>
            (close, open, high, low, volume, (if (future > close) 1 else 0))  // new DataSet(Nd4j.create(Array(close, open, high, low, volume)), Nd4j.create(Array(if (future > close) 1.0 else -1.0)))
        })

        // normalize TODO: DRY
        val (mean_open, std_open) = trainCSV.select(mean("open"), stddev("open"))
          .as[(Double, Double)]
          .first()
        val (mean_high, std_high) = trainCSV.select(mean("high"), stddev("high"))
          .as[(Double, Double)]
          .first()
        val (mean_low, std_low) = trainCSV.select(mean("low"), stddev("low"))
          .as[(Double, Double)]
          .first()
        val (mean_close, std_close) = trainCSV.select(mean("close"), stddev("close"))
          .as[(Double, Double)]
          .first()
        val (mean_volume, std_volume) = trainCSV.select(mean("volume"), stddev("volume"))
          .as[(Double, Double)]
          .first()

        val normalized = trainCSV
          .withColumn("volume", ($"volume" - mean_volume) / std_volume)
          .withColumn("close", ($"close" - mean_close) / std_close)
          .withColumn("open", ($"open" - mean_open) / std_open)
          .withColumn("high", ($"high" - mean_high) / std_high)
          .withColumn("low", ($"low" - mean_low) / std_low)

        val filename = s"preprocessed_$i.csv"
        val outputFileName = outputdir + "/temp_" + filename
        val mergedFileName = outputdir + "/merged_" + filename

        normalized.write
          .format("com.databricks.spark.csv")
          .option("header", "false")
          .mode("overwrite")
          .save(s"hdfs://namenode:9000$outputFileName")
        copyMerge(hdfs, new Path(outputFileName), new Path(mergedFileName), true, hadoopConfig)
        trainCSV.unpersist()
      }

      val outputTrainDir = "/topics/stock_test/partition=0/preprocessed"
      val outputEvalDir = "/topics/stock_test/partition=0/evaluation"
      createDir(outputTrainDir)
      createDir(outputEvalDir)

      // Preprocess training set
      for (i <- 0 until 30) {
        println(i)
        preprocessData(stockFolder + s"/stock_test+0+00000000${"%02d".format(i)}+00000000${"%02d".format(i)}.json", outputTrainDir, i)
      }

      // Preprocess evaluation set
      for (i <- 30 until 50) {
        preprocessData(stockFolder + s"/stock_test+0+00000000${"%02d".format(i)}+00000000${"%02d".format(i)}.json", outputEvalDir, i)
      }

      // LOAD TRAINING SET
      val origData = spark.sparkContext.binaryFiles(s"hdfs://namenode:9000$outputTrainDir")
      val numberOfHeaders = 0
      val delimeter = ","
      val seqRR = new CSVSequenceRecordReader(numberOfHeaders, delimeter)
      val sequenceRDD = origData.toJavaRDD.map(new SequenceRecordReaderFunction(seqRR))
      val dataSet = sequenceRDD.map(new DataVecSequenceDataSetFunction(LABEL_INDEX, NUM_CLASSES, false))

      // LOAD EVALUATION SET
      val evalData = spark.sparkContext.binaryFiles(s"hdfs://namenode:9000$outputEvalDir")
      val sequenceEvalRDD = evalData.toJavaRDD.map(new SequenceRecordReaderFunction(seqRR))
      val evalDataSet = sequenceEvalRDD.map(new DataVecSequenceDataSetFunction(LABEL_INDEX, NUM_CLASSES, false))


      // NETWORK CONFIGURATION
      val lowLevelConf = VoidConfiguration.builder()
        .unicastPort(40123) //Port that workers will use to communicate. Use any free port
        .controllerAddress("172.24.0.15") //IP of the master/driver
        .build()

      val trainingMaster = new SharedTrainingMaster.Builder(lowLevelConf, dataSet.count().toInt)
        .batchSizePerWorker(BATCH_SIZE)
        .thresholdAlgorithm(new AdaptiveThresholdAlgorithm(5))
        .residualPostProcessor(new ResidualClippingPostProcessor(5, 5))
        .rddTrainingApproach(RDDTrainingApproach.Direct)
        .encodingDebugMode(true)
        .workersPerNode(3)
        .build()

      val conf = new NeuralNetConfiguration.Builder()
        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
        .dropOut(DROPOUT_RATE)
        .updater(new Nadam(LEARNING_RATE))
        .list()
        .layer(new LSTM.Builder()
          .nIn(FEATURE_SIZE)
          .nOut(NUM_NEURONS)
          .activation(Activation.SOFTMAX)
          .build())
        .layer(new RnnOutputLayer.Builder()
          .nIn(NUM_NEURONS)
          .nOut(2)
          .weightInit(WeightInit.XAVIER)
          .activation(Activation.SOFTMAX)
          .lossFunction(LossFunction.MCXENT)
          .build())
        .backpropType(BackpropType.TruncatedBPTT)
        .tBPTTLength(70)
        .build()

      // TRAIN
      val sparkNet = new SparkDl4jMultiLayer(spark.sparkContext, conf, trainingMaster)

      // Initiate tracking
      val flowClient = new MlflowClient("https://mlflow:5000")
      val experimentName = "ONE_LAYER_LSTM_SOFTMAX_MCXENT"
      val previousExperiment = flowClient.getExperimentByName(experimentName)
      var experiment = ""
      if(previousExperiment.isPresent) {
        experiment = previousExperiment.get().getExperimentId
      } else {
        experiment = flowClient.createExperiment(experimentName)
      }

      val run = flowClient.createRun(experiment)

      flowClient.logParam(run.getRunId, "learning_rate", LEARNING_RATE.toString)
      flowClient.logParam(run.getRunId, "num_neurons", NUM_NEURONS.toString)
      flowClient.logParam(run.getRunId, "dropout_rate", DROPOUT_RATE.toString)
      flowClient.logParam(run.getRunId, "look_ahead", LOOK_AHEAD.toString)

      var i = 0
      var network: SparkDl4jMultiLayer = null
      while (i <= NUM_EPOCHS) {
        val evaluation: Evaluation = sparkNet.evaluate(evalDataSet)
        flowClient.logMetric(run.getRunId, "accuracy", evaluation.accuracy(), System.currentTimeMillis(), i)
        flowClient.logMetric(run.getRunId, "precision", evaluation.precision(), System.currentTimeMillis(), i)
        flowClient.logMetric(run.getRunId, "recall", evaluation.recall(), System.currentTimeMillis(), i)
        flowClient.logMetric(run.getRunId, "f1", evaluation.f1(), System.currentTimeMillis(), i)
        flowClient.logMetric(run.getRunId, "false_negatives", evaluation.getFalseNegatives().totalCount(), System.currentTimeMillis(), i)
        flowClient.logMetric(run.getRunId, "false_positives", evaluation.getFalsePositives().totalCount(), System.currentTimeMillis(), i)
        flowClient.logMetric(run.getRunId, "true_negatives", evaluation.getTrueNegatives().totalCount(), System.currentTimeMillis(), i)
        flowClient.logMetric(run.getRunId, "true_positives", evaluation.getTruePositives().totalCount(), System.currentTimeMillis(), i)
        i += 1
      }

      // SAVE MODEL
      val os = new BufferedOutputStream(hdfs.create(new Path("/topics/stock_test/partition=0/model.bin")))
      ModelSerializer.writeModel(sparkNet.getNetwork, os, true)
    } catch {
      case e: UnresolvedAddressException => e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
}
