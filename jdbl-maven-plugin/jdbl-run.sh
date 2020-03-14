#!/bin/env zsh

#echo "----- INSTALLING THE jdbl PLUGIN -----"
#mvn clean
#mvn install -DskipTests=true


echo "----- RUNNING THE EXPERIMENTS ON dummy-project -----"
cd src/it/commons-cli
mvn clean package
