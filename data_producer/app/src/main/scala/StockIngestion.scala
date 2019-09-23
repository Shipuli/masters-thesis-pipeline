import java.util.Properties
import java.util.concurrent.Future

import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.admin._

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.collection.JavaConverters._

import play.api.libs.json._

object StockIngestion extends App {

  val  props = new Properties()

  val kafkaHost = sys.env.getOrElse("KAFKA_HOST", "localhost")
  val kafkaPort = sys.env.getOrElse("KAFKA_PORT", "9092")
  props.put("bootstrap.servers", s"$kafkaHost:$kafkaPort")

  val adminClient = AdminClient.create(props)

  val numPartitions = 1
  val replicationFactor = 1.toShort
  val TOPIC="stock_test"

  val topics = adminClient.listTopics().names().get()
  if (!topics.contains(TOPIC)) {
    val newTopic = new NewTopic(TOPIC, numPartitions, replicationFactor)
    adminClient.createTopics(List(newTopic).asJavaCollection)
  }

  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)

  val n_of_records = 50
  val records = ArrayBuffer.empty[Future[RecordMetadata]]

  val dataDir = sys.env.getOrElse("DATA_DIR", System.getProperty("user.dir") + "/data")
  val files = Utils.getListOfFiles(dataDir + "/stocks")
  val testFiles = files.take(n_of_records)

  for (file <- testFiles) {
    val valueSource = Source.fromFile(file)
    val value = valueSource.getLines.mkString
    valueSource.close()
    val key = file.getName.split("_").head

    val parsedValue: List[JsObject] = Json.parse(value).as[List[JsObject]]
    val outputValue = parsedValue map(o => o + ("symbol" -> Json.toJson(key)))

    val record = new ProducerRecord(TOPIC, key, Json.stringify(Json.toJson(outputValue)))
    records += producer.send(record)
  }

  records.foreach(r => {
    try {
      r.get()
    } catch {
      case throwable: Throwable => println("WHAAAAT: " + throwable)
    }
  })

  producer.close()
}
