# !/bin/bash

docker container stop hdfs_historyserver_1
docker container stop hdfs_datanode_1
docker container stop hdfs_namenode_1
docker container stop hdfs_nodemanager_1
docker container stop hdfs_resourcemanager_1
docker container list
docker container prune 
docker network prune
docker volume prune
