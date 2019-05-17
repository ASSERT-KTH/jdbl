#!/usr/bin/env bash

cd jdbl-tracer-maven-plugin

echo "----- INSTALLING THE dbl PLUGIN -----"
mvn clean
mvn -q install
cd ..

echo "----- RUNNING THE EXPERIMENTS on clitools -----"
#cd experiments/dummy-project
#cd experiments/clitools
cd experiments/ttorrent
mvn clean
mvn -q install -DskipTests

#echo "----- EXECUTING DEBLOATED clitools -----"
#cd target/
#java -jar clitools-1.0.0-SNAPSHOT.jar woami