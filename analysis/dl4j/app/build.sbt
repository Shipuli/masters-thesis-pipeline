import sbt._

name := "app"

version := "0.1"

scalaVersion := "2.11.12"

val sparkVersion = "2.4.3"

val hadoopVersion = "3.2.0"

val nd4jVersion = "1.0.0-beta5"

val dl4jVersion = "1.0.0-beta5"

val datavecVersion = "1.0.0-beta5"

val assemblyName = s"analyzer-assembly"

javaOptions in run ++= Seq(
  "-Dlog4j.debug=true",
  "-Dlog4j.configuration=log4j.properties")

val isALibrary = true

val sparkExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.apache.hadoop", "hadoop-client").
    exclude("org.apache.hadoop", "hadoop-yarn-client").
    exclude("org.apache.hadoop", "hadoop-yarn-api").
    exclude("org.apache.hadoop", "hadoop-yarn-common").
    exclude("org.apache.hadoop", "hadoop-yarn-server-common").
    exclude("org.apache.hadoop", "hadoop-yarn-server-web-proxy")


val assemblyDependencies = (scope: String) => Seq(
  sparkExcludes("org.nd4j" % "nd4j-native-platform" % nd4jVersion % scope)
    exclude("com.fasterxml.jackson.core", "jackson-annotations")
    exclude("com.fasterxml.jackson.core", "jackson-core")
    exclude("com.fasterxml.jackson.core", "jackson-databind")
    exclude("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml"),
  sparkExcludes("org.deeplearning4j" %% "dl4j-spark" % s"${dl4jVersion}" % scope)
    exclude("org.apache.spark", "*")
    exclude("com.fasterxml.jackson.core", "jackson-annotations")
    exclude("com.fasterxml.jackson.core", "jackson-core")
    exclude("com.fasterxml.jackson.core", "jackson-databind")
    exclude("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml"),
  sparkExcludes("org.nd4j" %% "nd4j-kryo" % nd4jVersion % scope)
    exclude("com.esotericsoftware.kryo", "kryo"),
  sparkExcludes("org.nd4j" %% "nd4s" % nd4jVersion % scope),
  sparkExcludes("org.datavec" % "datavec-api" % datavecVersion % scope),
  sparkExcludes("org.deeplearning4j" %% "dl4j-spark-parameterserver" % dl4jVersion % scope)
    exclude("org.apache.spark", "*")
    exclude("com.fasterxml.jackson.core", "jackson-annotations")
    exclude("com.fasterxml.jackson.core", "jackson-core")
    exclude("com.fasterxml.jackson.core", "jackson-databind")
    exclude("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml"),
  sparkExcludes("org.datavec" %% "datavec-spark" % s"${datavecVersion}" % scope)
    exclude("org.apache.spark", "*"),
  sparkExcludes("org.bytedeco" % "javacpp" % "1.5.1-1" % scope),
  sparkExcludes("org.bytedeco" % "mkl-dnn" % "0.20-1.5.1" % scope),
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.4" % scope,
  "com.fasterxml.jackson.core" % "jackson-core" % "2.4.4" % scope,
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4" % scope,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.4.4" % scope
)

val hadoopClientExcludes =
  (moduleId: ModuleID) => moduleId.
    exclude("org.slf4j", "slf4j-api").
    exclude("javax.servlet", "servlet-api")

lazy val assemblyDependenciesScope: String = if (isALibrary) "compile" else "provided"

lazy val hadoopDependenciesScope = if (isALibrary) "provided" else "compile"

libraryDependencies ++= Seq(
  sparkExcludes("org.apache.spark" %% "spark-core" % sparkVersion % "provided")
      exclude("org.eclipse.jetty.orbit", "javax.servlet")
      exclude("org.eclipse.jetty.orbit", "javax.transaction")
      exclude("org.eclipse.jetty.orbit", "javax.mail")
      exclude("org.eclipse.jetty.orbit", "javax.activation")
      exclude("commons-beanutils", "commons-beanutils-core")
      exclude("commons-collections", "commons-collections")
      exclude("commons-collections", "commons-collections")
      exclude("com.esotericsoftware.minlog", "minlog"),
  sparkExcludes("org.apache.spark" %% "spark-sql" % sparkVersion % "provided"),
  sparkExcludes("org.apache.spark" %% "spark-yarn" % sparkVersion % "provided"),
  hadoopClientExcludes("org.apache.hadoop" % "hadoop-yarn-api" % hadoopVersion % "provided"),
  hadoopClientExcludes("org.apache.hadoop" % "hadoop-yarn-client" % hadoopVersion % "provided"),
  hadoopClientExcludes("org.apache.hadoop" % "hadoop-yarn-common" % hadoopVersion % "provided"),
  hadoopClientExcludes("org.apache.hadoop" % "hadoop-yarn-applications-distributedshell" % hadoopVersion % "provided"),
  hadoopClientExcludes("org.apache.hadoop" % "hadoop-yarn-server-web-proxy" % hadoopVersion % "provided"),
  hadoopClientExcludes("org.apache.hadoop" % "hadoop-client" % hadoopVersion % "provided")
) ++ assemblyDependencies(assemblyDependenciesScope)

classpathTypes += "maven-plugin"

assemblyMergeStrategy in assembly := {
  case PathList("javax", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.last
  case PathList("com", "google", xs @ _*) => MergeStrategy.last
  case PathList("io.netty", xs @ _*) => MergeStrategy.first
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last startsWith "module-info" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last startsWith "io.netty.versions" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
