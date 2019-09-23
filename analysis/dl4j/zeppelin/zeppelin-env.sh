export SPARK_HOME=/zeppelin/spark-2.4.3-bin-hadoop2.7
export SPARK_SUBMIT_OPTIONS="--conf spark.hadoop.fs.defaultFS=hdfs://namenode:9000 --num-executors 2 --executor-cores 2 --executor-memory 4G --driver-memory 4G --conf spark.executor.extraJavaOptions=-Dorg.bytedeco.javacpp.maxbytes=4G --conf spark.driver.extraJavaOptions=-Dorg.bytedeco.javacpp.maxbytes=4G"
export ZEPPELIN_INTP_MEM="-Xmx4g"
