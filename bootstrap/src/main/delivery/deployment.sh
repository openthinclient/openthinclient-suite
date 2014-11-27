#!/bin/sh
# usage: deplyoment.sh <environment> <goal>

cd groovy

CP="-cp .:groovy-all.jar:mysql-connector-java.jar:liquibase-core.jar:commons-io.jar:commons-lang3.jar"
echo "Shell - Directory Current: `pwd`"
echo "Shell - ClassPath: $CP"
echo "Shell - environment: $1"
echo "Shell - goal: $2"
echo "Shell - Execute: java $CP com.t_systems_mms.health.deployment.GroovyRun $1 $2"
java $CP com.t_systems_mms.health.deployment.GroovyRun $1 $2

cd ..