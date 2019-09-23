FROM apache/zeppelin:0.8.1

RUN apt-get update && apt-get install -y curl 
RUN apt-get update && apt-get install -y maven
RUN rm -rf /zeppelin/local-repo/commons-lang
RUN curl https://archive.apache.org/dist/spark/spark-2.4.3/spark-2.4.3-bin-hadoop2.7.tgz --output spark.tgz
RUN tar xvf spark.tgz
RUN rm spark.tgz

#COPY ./interpreter.json /zeppelin/conf/interpreter.json
COPY ./zeppelin-env.sh /zeppelin/conf/zeppelin-env.sh
#COPY ./spark-defaults.conf /zeppelin/spark-2.4.3-bin-hadoop2.7/conf/spark-defaults.conf
COPY ./spark-env.sh /zeppelin/spark-2.4.3-bin-hadoop2.7/conf/spark-env.sh
RUN mkdir -p /zeppelin/spark-2.4.3-bin-hadoop2.7/hadoop_conf
COPY ./core-site.xml /zeppelin/spark-2.4.3-bin-hadoop2.7/hadoop_conf/core-site.xml

ENV DL4J_VOID_IP 172.24.0.15

EXPOSE 4040 40123 49876
