# Master's thesis pipeline

## Overview

*INSERT HERE GRAPH ABOUT THE SYSTEM*

## Running the system with docker-compose

(0. have docker-compose installed)

1. `docker-compose up`

## Running the system on local machine

### Prerequisities

1. Mongodb (`docker run --name my_mongo -d -p 127.0.0.1:27017:27017 mongo:latest`)

### Worker

Getting started to populate data for backend to serve.

Requirements: Python 3.7

1. cd worker
2. Install dependencies with pip

Now depending how you want to run the worker there are 2 options:

1. `python cli.py` which is a command line api for the different population options. Check cli.py main-method for different arguments the program takes.
2. `export FLASK_APP=server.py && export FLASK_ENV=development && python -m flask run` which is a http server api for the same options. Implements http interface for the population methods.

### Server

NodeJS server which serves the data

Requirements: Nodejs 8.11.2

1. yarn
2. yarn start