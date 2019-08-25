import java.nio.channels.UnresolvedAddressException

import org.apache.spark.SparkConf
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.datavec.api.split.NumberedFileInputSplit
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.{BackpropType, NeuralNetConfiguration}
import org.deeplearning4j.nn.conf.layers.{LSTM, RnnOutputLayer}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Nadam

object StockAnalysis {

  val stockFolder = "hdfs://namenode:9000/topics/stock_test/partition=0"

  def preprocessValues(start: Int, end: Int, spark: SparkSession): Unit = {
    import spark.implicits._
    // train set
    for (i <- start until end) {
      val loadStock = spark.read.json(stockFolder + s"/stock_test+0+00000000${"%02d".format(i)}+00000000${"%02d".format(i)}.json")
      val window = Window.orderBy("date")
      val indexAppended = loadStock.withColumn("index", row_number().over(window))
      val selfJoined = indexAppended.as("d1")
        .join(indexAppended.as("d2")
          .select("close", "index")
          .withColumnRenamed("close", "future"),
          $"d1.index" === $"d2.index" + 5)
        .select($"d1.close".as("close"), $"open", $"high", $"low", $"volume", $"future")
      // val trainSet :: validationSet :: testSet :: _ = selfJoined.randomSplit(Array(0.6, 0.2, 0.2)).toList
      /*
      val validationLabels = validationSet.map {
        case Row(close: Double, open: Double, high: Double, low: Double, volume: Long, future: Double) => if (future > close) 1 else -1
      }
      val testLabels = testSet.map {
        case Row(close: Double, open: Double, high: Double, low: Double, volume: Long, future: Double) => if (future > close) 1 else -1
      }*/


      val trainCSV: Dataset[String] = selfJoined.map{
        case Row(close: Double, open: Double, high: Double, low: Double, volume: Long, future: Double)
        => s"$close,$open,$high,$low,$volume,${if (future > close) 1 else -1}"
      }
      val outPath = stockFolder + s"/preprocessed_train_stock_$i.csv"
      trainCSV.rdd.saveAsTextFile(outPath)
    }
  }

  def main(args: Array[String]): Unit = {
    val hdfsMaster = "hdfs://namenode:9000"

    val conf = new SparkConf()
    conf.setMaster("local")
    conf.setAppName("")
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
        **/
      val LOOK_AHEAD = 5
      val NUM_EPOCHS = 10
      val LEARNING_RATE = 0.002
      val BATCH_SIZE = 70
      val NUM_NEURONS = 10
      val FEATURE_SIZE = 5
      val DROPOUT_RATE = 0.7
      val NUM_CLASSES = 2
      val LABEL_INDEX = 5

      preprocessValues(0, 10, spark)

      // validation set

      val reader = new CSVSequenceRecordReader(1, ",")
      reader.initialize(new NumberedFileInputSplit(s"$hdfsMaster/preprocessed_train_stock_%d.csv", 0, 34))
      val iterClassification = new SequenceRecordReaderDataSetIterator(reader, BATCH_SIZE, NUM_CLASSES, LABEL_INDEX, false)

      //val trainData: JavaRDD[List[Writable]] = trainTmp.map(new StringToWritablesFunction(recordReader).asInstanceOf[Function[String, List[Writable]]])
      //val rddDataSet: JavaRDD[DataSet] = trainData.map(new DataVecDataSetFunction(5, NUM_CLASSES, false).asInstanceOf[Function[List[Writable], DataSet]]).toJavaRDD()

      //println(trainSet.count())
      //println(validationSet.count())
      //println(testSet.count())

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
          .nOut(1)
          .weightInit(WeightInit.XAVIER)
          .activation(Activation.SOFTMAX)
          .build())
        .backpropType(BackpropType.TruncatedBPTT)
        .tBPTTLength(70)
        .build()

      val network = new MultiLayerNetwork(conf)

      network.fit(iterClassification)

    } catch {
      case e: UnresolvedAddressException => e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
}
