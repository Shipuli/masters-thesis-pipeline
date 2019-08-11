import java.nio.channels.UnresolvedAddressException

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object StockAnalysis {

  def main(args: Array[String]): Unit = {
    val hdfsMaster = "hdfs://namenode:9000"

    val conf = new SparkConf()
    conf.setMaster("local")
    conf.setAppName("")
    val spark = SparkSession.builder.config("spark.hadoop.dfs.client.use.datanode.hostname", "true").config("spark.hadoop.dfs.datanode.https.address", "datanode:9864").appName("Stock Analysis").getOrCreate()
    try {
      val stockData = spark.read.json(hdfsMaster + "/topics/stock_test/partition=0/stock_test+0+0000000000+0000000000.json")
      stockData.printSchema()
    } catch {
      case e: UnresolvedAddressException => e.printStackTrace()
    } finally {
      spark.stop()
    }
  }
}
