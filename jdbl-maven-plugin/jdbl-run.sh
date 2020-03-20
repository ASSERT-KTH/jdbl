#!/bin/env zsh

#echo "----- INSTALLING THE jdbl PLUGIN -----"
cd ..
mvn clean
mvn install -DskipTests=true


echo "----- RUNNING THE EXPERIMENTS ON dummy-project -----"
cd jdbl-maven-plugin/src/it/dummy-project
mvn clean package -e
