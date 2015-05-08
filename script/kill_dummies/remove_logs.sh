#!/bin/bash

if [ $# != 1 ]; then
    echo "I need an IP file"
    exit 1
fi

USER=milax
PASS=milax
LOG_PATH="~/startImpl/logs"

for IP in $(cat $1)
do
	sshpass -p $PASS ssh -o StrictHostKeyChecking=no milax@$IP "rm -rf $LOG_PATH"
done
