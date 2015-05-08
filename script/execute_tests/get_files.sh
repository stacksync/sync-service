#!/bin/bash

if [ $# != 2 ]; then
    echo "script <IP> <FOLDER>"
    exit 1
fi

USER=milax
PASS=milax
LOG_PATH="~/startImpl/logs/sync-server.log"


mkdir -p $2

i=0
for IP in $(cat $1)
do
	echo $IP
	sshpass -p $PASS scp -o StrictHostKeyChecking=no milax@$IP:$LOG_PATH $2/log$IP
done
