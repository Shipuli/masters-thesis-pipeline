FROM bitnami/kafka:2.3.0

COPY connect-standalone.properties /opt/bitnami/kafka
USER root
RUN apt-get update && apt-get install -y netcat
RUN mkdir -p /kafka/plugins
RUN chown 1001 /kafka/plugins

ADD kafka-connect-hdfs /kafka/plugins/kafka-connect-hdfs

ADD entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENV KAFKA_HEAP_OPTS="-Xms512m -Xmx1g"

ENTRYPOINT [ "/entrypoint.sh" ]
CMD ["/entrypoint.sh"]