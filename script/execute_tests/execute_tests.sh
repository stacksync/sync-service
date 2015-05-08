#!/bin/bash

if [ $# != 2 ]; then
    echo "USAGE ERROR:"
    echo -e "script <threads_users_cs_min> <IP>"
    exit 1
fi

properties=$1
IP=$2

i=1
while read line
do
    echo "Start experiment $i"
    num_threads=$(echo $line | awk '{print $1}')
    users=$(echo $line | awk '{print $2}')
    commits_second=$(echo $line | awk '{print $3}')
    minutes=$(echo $line | awk '{print $4}')

    commits=$(($commits_second * 60))

    echo "Executing test $i. num_threads=$num_threads, num_users=$users, commits_minute=$commits, minutes=$minutes"
    # copy the experiment in the different machines
    ./scp_start_wait.sh $IP $num_threads $users $commits $minutes > /dev/null 2>&1

    sleep 30

    echo "Start experiment"
    # start the experiment
    ./start_dummy.sh > /dev/null 2>&1

    nap=$((($minutes+1)*60))

    echo "Sleep $nap seconds"
    # wait until the experiment ends
    sleep $nap

    echo "Recovering files"
    # recover all the information about the experiments
    folder="experiment$i"
    ./get_files.sh $IP $folder > /dev/null 2>&1

    echo "Remove and kill dummies"
    # remove and kill dummies
    ./kill_java.sh $IP > /dev/null 2>&1
    ./remove_dummies.sh $IP > /dev/null 2>&1

    echo "Cleaning db"
    # clean db
    ./delete_db_users.sh > /dev/null 2>&1
    
    echo $properties

    ((i++))
done < $properties

