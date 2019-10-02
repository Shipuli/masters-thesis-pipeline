#!/usr/bin/env bash
echo $BASH_VERSION

mkdir -p build

ingestion="{waiting input}"
storage="{waiting input}"
analysis="{waiting input}"
services=""

add_service() {
  local path=$1
  local destination="./build/$2"
  local silence=$3
  local ignore=$4
  # Copy docker-compose config
  local shard=""
  local index=0
  while IFS= read -r line; do
    first_char="${line:0:1}"
    if ! ( echo "$first_char" | grep -q ' ' ); then
      if [[ "$silence" = true && !( "$index" == 0 ) ]]; then
        shard="$shard    logging:\n      driver: none\n"
      fi 
    fi
    ((index++))
    shard="$shard  $line\n"
  done < "$path/compose-shard.yml"

  if [ "$silence" = true ]; then
    shard="$shard    logging:\n      driver: none\n"
  fi
  
  services="$services$shard"
  # Copy files to build folder
  mkdir -p "$destination"
  if [ -z "$ignore" ]; then 
    rsync -aR "$path/./" "$destination/"
  else
    local ignorestring=""
    for exc in $ignore; do
      if [ -z "$ignorestring" ]; then
        ignorestring="--exclude=$exc"
      else
        ignorestring="$ignorestring --exclude=$exc"
      fi
    done
    rsync -aR $(echo $ignorestring) "$path/./" "$destination/"
  fi

  rm "$destination/compose-shard.yml"
}

if [[ $# -ne 3 ]]; then
  PS3='Select ingestion technology: '
  options=("Flume (Deprecated)" "Kafka" "Quit")
  select opt in "${options[@]}"
  do
      case $opt in
          "Flume")
              ingestion="Flume"
              echo "Your current pipeline: $ingestion ==> $storage ==> $analysis"
              echo ""
              break
              ;;
          "Kafka")
              ingestion="Kafka"
              echo "Your current pipeline: $ingestion ==> $storage ==> $analysis"
              echo ""
              break
              ;;
          "Quit")
              exit 1
              break
              ;;
          *) echo "invalid option $REPLY";;
      esac
  done
  PS3='Select storage technology: '
  options=("Cassandra (Deprecated)" "HDFS" "Quit")
  select opt in "${options[@]}"
  do
      case $opt in
          "Cassandra")
              storage="Cassandra"
              echo "Your current pipeline: $ingestion ==> $storage ==> $analysis"
              echo ""
              break
              ;;
          "HDFS")
              storage="Hdfs"
              echo "Your current pipeline: $ingestion ==> $storage ==> $analysis"
              echo ""
              break
              ;;
          "Quit")
              exit 1
              break
              ;;
          *) echo "invalid option $REPLY";;
      esac
  done
  PS3='Select analysis technology: '
  options=("DL4J" "Quit")
  select opt in "${options[@]}"
  do
      case $opt in
          "DL4J")
              analysis="DL4J"
              echo "Your current pipeline: $ingestion ==> $storage ==> $analysis"
              echo ""
              break
              ;;
          "Quit")
              exit 1
              break
              ;;
          *) echo "invalid option $REPLY";;
      esac
  done

  # Remove previous build
  if [ -d "./build/ingestion" ]; then
    rm -rf ./build/ingestion
  fi
  if [ -d "./build/storage" ]; then
    rm -rf ./build/storage
  fi

  silence=false
  #read -p "Silence services? [y/n] " -n 1 -r
  #if [[ $REPLY =~ ^[Yy]$ ]]
  #then
  #  silence=true
  #fi

  if [ -d "data_producer" ]; then
    excluded="*target* *.idea*"
    add_service 'data_producer' data_producer "$silence" "$excluded"
    echo "Copying the data"
    mkdir -p "./build/data_producer/app/data"
    cp -R -n "./data/." "./build/data_producer/app/data"
  fi

  # Add service specific parts
  if [[ "$ingestion" == "Flume" ]]; then
    add_service "ingestion/flume" "ingestion" "$silence"
  fi

  if [[ "$ingestion" == "Kafka" ]]; then
    if [ ! -d "./ingestion/kafka/connect/kafka-connect-hdfs" ]; then
      echo "Missing kafka HDFS connect plugin!"
      echo "Visit https://www.confluent.io/connector/kafka-connect-hdfs/#download and unzip the content of the only folder in the download into ingestion/kafka/kafka-connect-hdfs"
      read -p "Type 'y' when done " -n 1 -r
      echo    # (optional) move to a new line
      if [[ ! $REPLY =~ ^[Yy]$ ]]
      then
          [[ "$0" = "$BASH_SOURCE" ]] && exit 1 || return 1
      fi
    fi
    add_service "ingestion/kafka" "ingestion" "$silence"
  fi

  if [[ "$storage" == "Cassandra" ]]; then
    add_service "storage/cassandra" "storage" "$silence"
  fi

  if [[ "$storage" == "Hdfs" ]]; then
    add_service "storage/hdfs" "storage" "$silence"
  fi

  if [[ "$analysis" == "DL4J" ]]; then
    excluded="*target* *.idea*"
    add_service "analysis/dl4j" "analysis" "$silence" "$excluded"
  fi

  # Remove last useless line break
  services=${services::${#services}-2}

  # Change to lower case 
  if [[ "$storage" == "Hdfs" ]]; then
    services="${services//\{\{storage\}\}/namenode}"
  else
    storage=`echo "$storage" | tr '[:upper:]' '[:lower:]'`
    services="${services//\{\{storage\}\}/$storage}"
  fi


  ingestion=`echo "$ingestion" | tr '[:upper:]' '[:lower:]'`
  services="${services//\{\{ingestion\}\}/$ingestion}"

  

  # Finally write new docker-compose file  
  output=`cat ./assets/docker-compose.yml`
  output="${output//\{\{services\}\}/$services}"
  printf "$output" > "./build/docker-compose.yml"
fi