#!/usr/bin/env bash

echo "----- INSTALLING THE jdbl PLUGIN -----"
mvn clean
mvn -q install -DskipTests=true

echo "----- RUNNING THE EXPERIMENTS ON dummy-project -----"
cd src/it/dummy-project
mvn clean
mvn package
