# check user is sudo
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

# download rabbitmq
if ! grep "http://www.rabbitmq.com/debian/" /etc/apt/sources.list
then 
	echo "deb http://www.rabbitmq.com/debian/ testing main" >> /etc/apt/sources.list
fi

cd /tmp
wget https://www.rabbitmq.com/rabbitmq-signing-key-public.asc
apt-key add rabbitmq-signing-key-public.asc
apt-get update
apt-get -y install rabbitmq-server

# allow access guest guest
echo "[{rabbit, [{loopback_users, []}]}]." > /etc/rabbitmq/rabbitmq.config
# uncomment ulimit in the worst way possible 
cat /etc/default/rabbitmq-server | head -n -1 > /etc/default/rabbitmq-server_aux
mv /etc/default/rabbitmq-server_aux /etc/default/rabbitmq-server
echo "ulimit -n 1024" >> /etc/default/rabbitmq-server

# apply changes
service rabbitmq-server restart
