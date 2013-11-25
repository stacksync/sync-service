#!/usr/bin/env bash
if [[ $UID -ne 0 ]]; then
	echo "$0 must be run as root"
	exit 1
fi

if [ $# != 5 ]; then
    echo "USAGE ERROR:"
    echo -e "\tsudo $0 <rol> <TENANT> <USER> <PASSWORD> <QUOTA>"
    exit 1
fi

if ! [ -f "./settings.conf" ]; then
	echo "The file settings doesn't exist."
	exit 1
fi


ROLE=$1
TENANT=$2
USER=$3
SERVICE_PASSWORD=$4
QUOTA=$5

echo -e "command -> ./adduser.sh <rol=$ROLE> <TENANT=$TENANT> <USER=$USER> <PASSWORD=$SERVICE_PASSWORD> <QUOTA=$QUOTA>"

PROXY_IP=CHANGE_THIS

CONTROLLER_PUBLIC_ADDRESS=$PROXY_IP
CONTROLLER_ADMIN_ADDRESS=$PROXY_IP
CONTROLLER_INTERNAL_ADDRESS=$PROXY_IP

SERVICE_TOKEN=CHANGE_THIS

CONTROLLER_PUBLIC_ADDRESS=${CONTROLLER_PUBLIC_ADDRESS:-localhost}
CONTROLLER_ADMIN_ADDRESS=${CONTROLLER_ADMIN_ADDRESS:-localhost}
CONTROLLER_INTERNAL_ADDRESS=${CONTROLLER_INTERNAL_ADDRESS:-localhost}

TOOLS_DIR=$(cd $(dirname "$0") && pwd)
KEYSTONE_CONF=${KEYSTONE_CONF:-/etc/keystone/keystone.conf}
if [[ -r "$KEYSTONE_CONF" ]]; then
    EC2RC="$(dirname "$KEYSTONE_CONF")/ec2rc"
elif [[ -r "$TOOLS_DIR/../etc/keystone.conf" ]]; then
    # assume git checkout
    KEYSTONE_CONF="$TOOLS_DIR/../etc/keystone.conf"
    EC2RC="$TOOLS_DIR/../etc/ec2rc"
else
    KEYSTONE_CONF=""
    EC2RC="ec2rc"
fi

# Extract some info from Keystone's configuration file
if [[ -r "$KEYSTONE_CONF" ]]; then
    CONFIG_SERVICE_TOKEN=$(sed 's/[[:space:]]//g' $KEYSTONE_CONF | grep ^admin_token= | cut -d'=' -f2)
    CONFIG_ADMIN_PORT=$(sed 's/[[:space:]]//g' $KEYSTONE_CONF | grep ^admin_port= | cut -d'=' -f2)

    ipAux=$(sed 's/[[:space:]]//g' $KEYSTONE_CONF | grep ^bind_host= | cut -d'=' -f2)
    if [[ "$ipAux" != "0.0.0.0" ]]; then
	CONTROLLER_PUBLIC_ADDRESS=$ipAux
	CONTROLLER_ADMIN_ADDRESS=$ipAux
	CONTROLLER_INTERNAL_ADDRESS=$ipAux
    fi

fi

export SERVICE_TOKEN=${SERVICE_TOKEN:-$CONFIG_SERVICE_TOKEN}
if [[ -z "$SERVICE_TOKEN" ]]; then
    echo "No service token found."
    echo "Set SERVICE_TOKEN manually from keystone.conf admin_token."
    exit 1
fi

export SERVICE_ENDPOINT=${SERVICE_ENDPOINT:-http://$CONTROLLER_PUBLIC_ADDRESS:${CONFIG_ADMIN_PORT:-35357}/v2.0}

function get_id () {
    echo `"$@" | grep ' id ' | awk '{print $4}'`
}

function find_tenant(){
    keystone tenant-list | grep "$1[\^| \ ]" | head -n1
}

function find_user(){	
    keystone user-list | grep "$1[\^| \ ]" | head -n1
}

function find_role(){
    keystone role-list | grep "$1[\^| \ ]" | head -n1
}

function get_id_tenant(){
   echo "$2"
}

### START THE SCRIPT ###
echo "Admin token -> $SERVICE_TOKEN Enpoint URL -> $SERVICE_ENDPOINT"

echo "find_tenant $TENANT"
existTenant=$(find_tenant $TENANT)
if [ -n "$existTenant" ]; then
   TENANTID=$(get_id_tenant $existTenant)
   echo "Found Tenant -> $TENANT id -> $TENANTID"
else
   TENANTID=$(get_id keystone tenant-create --name="$TENANT" --description "Description $TENANT" --enabled true)
   echo "Created Tenant -> $TENANT id -> $TENANTID"
fi

echo "find_user $USER"
existUser=$(find_user $USER)
if [ -n "$existUser" ]; then
   USERID=$(get_id_tenant $existUser)
   echo "Found User -> $USER id -> $USERID"
else
   USERID=$(get_id keystone user-create --tenant_id "$TENANTID" --name="$USER" --pass="$SERVICE_PASSWORD" --enabled true)
   echo "Created User -> $USER id -> $USERID"
fi

echo "find_user $ROLE"
existRole=$(find_role $ROLE)
if [ -n "$existRole" ]; then
   ROLEID=$(get_id_tenant $existRole)
   echo "Found Role -> $ROLE id -> $ROLEID"
else
   ROLEID=$(get_id keystone role-create --name="$ROLE")
   echo "Created Role -> $ROLE id -> $ROLEID"
fi

###TODO if user-role exist then not creates.
keystone user-role-add --user $USERID --role $ROLEID --tenant $TENANTID
keystone user-role-add --user-id $USERID --role-id $ROLEID --tenant-id $TENANTID

###LOGIN
output=$(curl -s -d '{"auth": {"passwordCredentials": {"username": "'$USER'", "password": "'$SERVICE_PASSWORD'"}, "tenantName":"'$TENANT'"}}' -H 'Content-type: application/json' http://$CONTROLLER_PUBLIC_ADDRESS:5000/v2.0/tokens)
echo "curl -> curl -s -d '{\"auth\": {\"passwordCredentials\": {\"username\": \"$USER\", \"password\": \"$SERVICE_PASSWORD\"}, \"tenantName\":\"$TENANT\"}}' -H 'Content-type: application/json' http://$CONTROLLER_PUBLIC_ADDRESS:5000/v2.0/tokens"

###GET Auth token and Storage Url
USERTOKEN=`echo $output | ruby -e "require 'rubygems'; require 'json'; puts JSON[STDIN.read]['access']['token']['id'];"`
STORAGEURL=`echo $output | ruby -e "require 'rubygems'; require 'json'; arrayJson = JSON[STDIN.read]['access']['serviceCatalog']; 
arrayJson.each do |object|
	if object['name'] == \"swift\"
		puts object['endpoints'][0]['publicURL'];
	end
end"`

CLOUDID=`echo $STORAGEURL | ruby -e "require 'rubygems'; puts STDIN.read.split('/').last;"`

echo "User token: $USERTOKEN"
echo "Storage Url: $STORAGEURL"
echo "Cloud id: $CLOUDID"

###PUT ACCOUNT
#curl -i -XPUT -k -H "X-Auth-Token: $USERTOKEN" $STORAGEURL

echo ""
echo "=========================="
echo "===Cleaning  containers==="
echo "=========================="

containers=$(curl -k -H "X-Auth-Token: $USERTOKEN" $STORAGEURL)
for container in $containers
do
	objects=$(curl -k -H "X-Auth-Token: $USERTOKEN" "$STORAGEURL/$container")
	i=0
	for object in $objects
	do
		let i=$i+1
		curl -XDELETE -i -k -H "X-Auth-Token: $USERTOKEN" "$STORAGEURL/$container/$object"
		echo "DELETE object($i) -> $STORAGEURL/$container/$object"
	done
	#curl -XDELETE -i -k -H "X-Auth-Token: $USERTOKEN" "$STORAGEURL/$container"
	echo "DELETE container -> $STORAGEURL/$container"
done


echo "=========================="
echo "===Initialize $DATABASE==="
echo "=========================="


DATABASE=`cat ./settings.conf | ruby -e "require 'rubygems'; require 'json'; puts JSON[STDIN.read]['backend'];"`

if [ "$DATABASE" == ""  ] || [ "$DATABASE" == "nil" ]; then
	echo "Can't load database from settings."
	exit 1
fi

if [ "$DATABASE" != "postgres"  ] && [ "$DATABASE" != "riak" ]; then
	echo "Can't load database from settings (set riak or postgres)."
	exit 1
fi



###Initialize database.
./$DATABASE/adduser.rb -i $CLOUDID -n $USER -q $QUOTA
./$DATABASE/addworkspace.rb -i $CLOUDID -p /

if [ "$DATABASE" == "riak" ]; then
	./$DATABASE/adduser_translate.rb -i $CLOUDID -u $TENANT:$USER
fi
