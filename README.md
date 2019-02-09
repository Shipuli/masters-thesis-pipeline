# Master's thesis pipeline

## Overview

*INSERT HERE GRAPH ABOUT THE SYSTEM*

## Prerequisities

1. Mongodb (`docker run --name my_mongo -d -p 127.0.0.1:27017:27017 mongo:latest`)

## Processor

Getting started to populate data for backend to serve.

Requirements: Python 3.7

1. Install dependencies with pip
2. `python ./processor/`

## Server

NodeJS server which serves the data

Requirements: Nodejs 8.11.2

1. yarn
2. yarn start