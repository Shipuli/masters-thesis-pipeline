docker container stop build_kafka_1
docker container stop build_zookeeper_1 
docker container stop build_historyserver_1
docker container stop build_resourcemanager_1
docker container stop build_datanode_1 
docker container stop build_namenode_1 
docker container stop build_nodemanager_1
docker container stop build_connect_1 
docker container stop spark-master
docker container stop spark-worker-1
docker container stop zeppelin
docker container stop analyzer 
docker container stop mlflow 

docker container list
docker container prune
docker network prune
docker volume prune