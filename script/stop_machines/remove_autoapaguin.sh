if [ $# != 1 ]; then
    echo "I need an IP file"
    exit 1
fi

USER=milax
PASS=milax

i=0
for IP in $(cat $1)
do
	echo $IP 
	sshpass -p $PASS ssh -o StrictHostKeyChecking=no milax@$IP "echo \"milax\" | sudo -S rm /etc/cron.d/autoapaguin"
done
