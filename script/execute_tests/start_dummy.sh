#!/bin/bash

USER=milax
PASS=milax
JAR_FILE=sync-server-db-0.0.1-SNAPSHOT-jar-with-dependencies.jar
MAIN_CLASS=com.stacksync.syncservice.startExperiment.StartExperiment


java -cp $JAR_FILE $MAIN_CLASS
