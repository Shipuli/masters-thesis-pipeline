name := "StockIngestion"

version := "0.1"

scalaVersion := "2.12.0"


// https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "2.3.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.7.3"
