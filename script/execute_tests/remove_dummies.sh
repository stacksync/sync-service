#!/bin/bash

if [ $# != 1 ]; then
    echo "I need an IP file"
    exit 1
fi

USER=milax
PASS=milax
LOG_PATH="~/startImpl"

for IP in $(cat $1)
do
	sshpass -p $PASS ssh -n -o StrictHostKeyChecking=no milax@$IP "rm -rf $LOG_PATH"
done
