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
2. `docker-compose up`

## Developing

Easy way to develop Spark application is to run the pipeline locally and add spark-submit script in to your $PATH. Then just build the application with `sbt package` in the application root directory and deploy the application to local spark instance with e.g `spark-submit --class *insert-main-object-name* --master spark://localhost:7077 target/*insert-jar-path*`
