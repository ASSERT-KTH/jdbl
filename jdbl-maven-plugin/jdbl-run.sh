#!/usr/bin/env bash

echo "----- INSTALLING THE jdbl PLUGIN -----"
mvn clean
mvn -q install -DskipTests=true

echo "----- RUNNING THE EXPERIMENTS ON dummy-project -----"
cd experiments/clitools
mvn clean
mvn -q package -DskipTests=true