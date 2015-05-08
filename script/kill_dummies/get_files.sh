#!/bin/bash

if [ $# != 1 ]; then
    echo "I need an IP file"
    exit 1
fi

USER=milax
PASS=milax
LOG_PATH="~/"

i=0
for IP in $(cat $1)
do
	echo $IP
	sshpass -p $PASS scp -o StrictHostKeyChecking=no milax@$IP:$LOG_PATH log$IP
done
