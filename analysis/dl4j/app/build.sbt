name := "app"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.3"
libraryDependencies += "org.apache.spark" %% "spark-avro" % "2.4.3"

libraryDependencies += "org.nd4j" % "nd4j-native-platform" % "1.0.0-beta4"
libraryDependencies += "org.nd4j" % "nd4j-api" % "1.0.0-beta4"
libraryDependencies += "org.nd4j" %% "nd4j-kryo" % "1.0.0-beta4"

libraryDependencies += "org.deeplearning4j" % "deeplearning4j-core" % "1.0.0-beta4"
libraryDependencies += "org.deeplearning4j" %% "dl4j-spark" % "1.0.0-beta4_spark_2"
libraryDependencies += "org.datavec" % "datavec-api" % "1.0.0-beta4"
libraryDependencies += "org.datavec" %% "datavec-spark" % "1.0.0-beta4_spark_2"


val yarnVer = "2.9.2"
Seq("client", "yarn-client", "yarn-api").map { name =>
  libraryDependencies += "org.apache.hadoop" % s"hadoop-$name" % yarnVer
}

javaOptions in run ++= Seq(
  "-Dlog4j.debug=true",
  "-Dlog4j.configuration=log4j.properties")


assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
