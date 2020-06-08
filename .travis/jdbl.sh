#!/usr/bin/env bash

mvn clean install -Ptravis
mvn jacoco:report coveralls:report --fail-never
mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.coverage.jacoco.xmlReportPaths=jdbl-core/target/site/jacoco/jacoco.xml,jdbl-maven-plugin/target/site/jacoco/jacoco.xml -Dsonar.projectKey=castor-software_jdbl



