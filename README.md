# Master's thesis pipeline

## Overview

[Dataflow](doc/dataflow.png)

## Pipeline builder

(0. `chmod +x pipeline.sh`)
1. `pipeline.sh`
2. Follow the wizard

After the wizard the pipeline should be available in ./build folder.

## Running the pipeline

(0. Have docker and docker-compose installed)
1. `cd build`
2. `docker-compose build`
3. `docker-compose up`

After the pipeline has finished starting up Zeppelin can be found running in localhost:8090 and other monitoring UIs in their own default ports.

## Stopping/Deleting the pipeline

In the build folder there are bash and powershell scripts to stop all the containers from running. Running them without arguments and answering the command prompts should stop and delete all the running instances.

## Developing

One way to develop Spark application is to run the pipeline locally and add spark-submit script in to your $PATH. Then just build the application with `sbt package` in the application root directory and deploy the application to local spark instance with e.g `spark-submit --class *insert-main-object-name* --master spark://localhost:7077 target/*insert-jar-path*`

Otherway is just use the configured zeppelin notebooks for testing before making the application fully operational.

### Updating Zeppelin configuration

Zeppelin is can be configured using the web ui. To persist these changes after container shutdown run `docker exec zeppelin cat /zeppelin/conf/interpreter.json > analysis/dl4j/zeppelin/interpreter.json` in project root. But this might introduce bugs and is not recommended.