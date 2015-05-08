#!/bin/bash

if [ $# != 3 ]; then
    echo "USAGE ERROR:"
    echo -e "script <IP_uuid_file> <commitsPerMinute> <minutes>"
    exit 1
fi

USER=milax
PASS=milax
JAR_FILE=sync-server-db-0.0.1-SNAPSHOT-jar-with-dependencies.jar
MAIN_CLASS=com.stacksync.syncservice.startExperimentImpl.StartImpl
BROKER_PROPERTIES=broker.properties
FOLDER=startImpl

i=0
while read line
do
    IP=$(echo $line | awk '{print $1}')
    uuid=$(echo $line | awk '{print $2}')

    echo "IP $IP"
    echo "nohup java -cp $JAR_FILE $MAIN_CLASS $2 $3 $4 $5> /dev/null 2>&1 &"

    sshpass -p $PASS ssh -n -f -o StrictHostKeyChecking=no milax@$IP "cd $FOLDER; echo \"java -cp $JAR_FILE $MAIN_CLASS $2 $3 $4 $5\" > run_dummy.sh; chmod +x run_dummy.sh; nohup ./run_dummy.sh > /dev/null 2>&1 &"
    #sshpass -p $PASS ssh -o StrictHostKeyChecking=no milax@$IP "mkdir -p $FOLDER; cd $FOLDER; java -cp $JAR_FILE $MAIN_CLASS $uuid $2 $3"

    ((i++))
    sleep 1
done < $1

