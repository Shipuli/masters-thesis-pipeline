FROM openjdk:8u222-jdk-stretch

RUN apt-get update && apt-get install -y curl
RUN curl https://sbt-downloads.cdnedge.bluemix.net/releases/v1.3.0/sbt-1.3.0.tgz --output sbt.tgz
RUN tar xvf sbt.tgz
RUN rm sbt.tgz

RUN curl https://archive.apache.org/dist/spark/spark-2.4.3/spark-2.4.3-bin-hadoop2.7.tgz --output spark.tgz
RUN tar xvf spark.tgz
RUN rm spark.tgz

ENV PATH="${PATH}:/sbt/bin"
ENV PATH="${PATH}:/spark-2.4.3-bin-hadoop2.7/bin"
ENV SPARK_HOME=/spark-2.4.3-bin-hadoop2.7

RUN mkdir -p app
COPY . /app

RUN cd /app && sbt assembly

CMD tail -f /dev/null