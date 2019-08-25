FROM apache/zeppelin:0.8.1

RUN apt-get update && apt-get install -y curl 
RUN curl http://mirror.netinch.com/pub/apache/spark/spark-2.4.3/spark-2.4.3-bin-hadoop2.7.tgz --output spark.tgz
RUN tar xvf spark.tgz
RUN rm spark.tgz

COPY ./interpreter.json /zeppelin/conf/interpreter.json
COPY ./zeppelin-env.sh /zeppelin/conf/zeppelin-env.sh