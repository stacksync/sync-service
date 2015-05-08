if [ $# != 2 ]; then
    echo "USAGE ERROR:"
    echo -e "script <threads_users_cs_min> <IP>"
    exit 1
fi

properties=$1
i=1
while read line
do
    num_threads=$(echo $line | awk '{print $1}')
    users=$(echo $line | awk '{print $2}')
    commits_second=$(echo $line | awk '{print $3}')
    minutes=$(echo $line | awk '{print $4}')

    commits=$(($commits_second * 60))

    echo "Executing test $i. num_threads=$num_threads, num_users=$users, commits_minute=$commits, minutes=$minutes"
   
   
    echo "Start experiment"
    # start the experiment
   nap=$((($minutes+1)*60))

    echo "Sleep $nap seconds"
   
    echo "Recovering files"
   
    echo "Remove and kill dummies"
   
    echo "Cleaning db"
    
    ((i++))
done < $properties

