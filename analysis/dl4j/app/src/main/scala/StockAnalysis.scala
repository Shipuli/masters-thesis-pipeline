package org.analyzer

import java.io.{BufferedOutputStream, IOException}
import java.nio.channels.UnresolvedAddressException

import org.apache.spark.SparkConf
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{Row, SparkSession}
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
import org.deeplearning4j.spark.datavec.DataVecSequenceDataSetFunction
import org.deeplearning4j.util.ModelSerializer

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
      val i = 0
      val loadStock = spark.read.json(stockFolder + s"/stock_test+0+00000000${"%02d".format(i)}+00000000${"%02d".format(i)}.json")
      val window = Window.orderBy("date")
      val indexAppended = loadStock.withColumn("index", row_number().over(window))
      val selfJoined = indexAppended.as("d1")
        .join(
          indexAppended.as("d2")
            .select("close", "index")
            .withColumnRenamed("close", "future"),
          $"d1.index" === $"d2.index" + 5)
        .select($"d1.close".as("close"), $"open", $"high", $"low", $"volume", $"future")

      val trainCSV = selfJoined.map({
        case Row(close: Double, open: Double, high: Double, low: Double, volume: Long, future: Double) =>
          (close, open, high, low, volume, (if (future > close) 1 else 0))  // new DataSet(Nd4j.create(Array(close, open, high, low, volume)), Nd4j.create(Array(if (future > close) 1.0 else -1.0)))
      }).limit(100)

      val outputfile = "/topics/stock_test/partition=0/preprocessed"
      val hadoopConfig = new Configuration()
      hadoopConfig.set("fs.defaultFS", "hdfs://namenode:9000")
      val hdfs = FileSystem.get(hadoopConfig)
      if (!hdfs.exists(new Path(outputfile))) {
        hdfs.mkdirs(new Path(outputfile))
      } else {
        hdfs.delete(new Path(outputfile), true)
        hdfs.mkdirs(new Path(outputfile))
      }

      val filename = s"preprocessed_$i.csv"
      val outputFileName = outputfile + "/temp_" + filename
      val mergedFileName = outputfile + "/merged_" + filename

      trainCSV.write
        .format("com.databricks.spark.csv")
        .option("header", "false")
        .mode("overwrite")
        .save(s"hdfs://namenode:9000$outputFileName")
      copyMerge(hdfs, new Path(outputFileName), new Path(mergedFileName), true, hadoopConfig)
      trainCSV.unpersist()

      // NETWORK CONFIGURATION
      val lowLevelConf = VoidConfiguration.builder()
        .unicastPort(40123) //Port that workers will use to communicate. Use any free port
        .controllerAddress("172.24.0.15") //IP of the master/driver
        .build()

      val trainingMaster = new SharedTrainingMaster.Builder(lowLevelConf, trainCSV.count().toInt)
        .batchSizePerWorker(BATCH_SIZE)
        .thresholdAlgorithm(new AdaptiveThresholdAlgorithm(5))
        .residualPostProcessor(new ResidualClippingPostProcessor(5, 5))
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
          .build())
        .backpropType(BackpropType.TruncatedBPTT)
        .tBPTTLength(70)
        .build()

      // LOAD DATA FROM DATABASE
      val origData = spark.sparkContext.binaryFiles(s"hdfs://namenode:9000$outputfile")
      val numberOfHeaders = 0
      val delimeter = ","
      val seqRR = new CSVSequenceRecordReader(numberOfHeaders, delimeter)
      val sequenceRDD = origData.toJavaRDD.map(new SequenceRecordReaderFunction(seqRR))
      val dataSet = sequenceRDD.map(new DataVecSequenceDataSetFunction(LABEL_INDEX, NUM_CLASSES, false))
      dataSet.collect().get(0)
      // TRAIN
      val sparkNet = new SparkDl4jMultiLayer(spark.sparkContext, conf, trainingMaster)
      val fitted = sparkNet.fit(dataSet)

      // SAVE MODEL
      val os = new BufferedOutputStream(hdfs.create(new Path("/topics/stock_test/partition=0/model.bin")))
      ModelSerializer.writeModel(fitted, os, true)
      println(fitted)
    } catch {
      case e: UnresolvedAddressException => e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
}
