#!/bin/bash
while ! nc -z namenode 9870; do sleep 3; done
cat /usr/local/hadoop-conf/hdfs-site.xml
cd /opt/bitnami/kafka

./bin/connect-standalone.sh connect-standalone.properties /kafka/plugins/kafka-connect-hdfs/etc/quickstart-hdfs.properties