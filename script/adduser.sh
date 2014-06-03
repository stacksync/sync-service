#!/bin/bash

if [ $# != 3 ]; then
    echo "USAGE ERROR:"
    echo -e "\tsudo $0 <USER> <PASSWORD> <EMAIL>"
    exit 1
fi

# Keystone info
export OS_USERNAME=SWIFT_ADMIN_USER
export OS_PASSWORD=SWIFT_ADMIN_USER_PASS
export OS_TENANT_NAME=SWIFT_ADMIN_TENANT
export OS_AUTH_URL=http://$IP:35357/v2.0

# StackSync admin user info
IP=IP_HERE
STACKSYNC_TENANT=STACKSYNC_TENANT_HERE
STACKSYNC_ADMIN_USER=STACKSYNC_ADMIN_USER_HERE
STACKSYNC_ADMIN_USER_PASS=STACKSYNC_ADMIN_USER_PASS_HERE

# StackSync database info (postgresql)
STACKSYNC_DB_HOST=localhost
STACKSYNC_DB=STACKSYNC_DB_HERE
STACKSYNC_USER=STACKSYNC_USER_HERE
export PGPASSWORD=STACKSYNC_DB_PASS_HERE

#Create stacksync user
keystone user-create --name $1 --tenant $STACKSYNC_TENANT --pass $2

# Login as the StackSync admin and get the token and the storage url
output=$(curl -s -d '{"auth": {"passwordCredentials": {"username": "'$STACKSYNC_ADMIN_USER'", "password": "'$STACKSYNC_ADMIN_USER_PASS'"}, "tenantName":"'$STACKSYNC_TENANT'"}}' -H 'Content-type: application/json' http://$IP:5000/v2.0/tokens)

ADMINTOKEN=`echo $output | ruby -e "require 'rubygems'; require 'json'; puts JSON[STDIN.read]['access']['token']['id'];"`
STORAGEURL=`echo $output | ruby -e "require 'rubygems'; require 'json'; arrayJson = JSON[STDIN.read]['access']['serviceCatalog']; 
arrayJson.each do |object|
	if object['name'] == \"swift\"
		puts object['endpoints'][0]['publicURL'];
	end
end"`

curl -k -i -X PUT -H "X-Auth-Token: $ADMINTOKEN" -H "X-Container-Read: $STACKSYNC_TENANT:$1" -H "X-Container-Write: $STACKSYNC_TENANT:$1" $STORAGEURL/$1

# Create DB content
psql -h $STACKSYNC_DB_HOST $STACKSYNC_DB $STACKSYNC_USER -c "INSERT INTO user1 (name, swift_user, swift_account, email, quota_limit, quota_used) VALUES ('$1', '$1', '${STORAGEURL##*/}', '$3', 0, 0);"

psql -h $STACKSYNC_DB_HOST $STACKSYNC_DB $STACKSYNC_USER -c "INSERT INTO workspace (latest_revision, owner_id, is_shared, swift_container, swift_url) VALUES (0, (SELECT id FROM user1 WHERE name='$1'), false, '$1', '$STORAGEURL');"

psql -h $STACKSYNC_DB_HOST $STACKSYNC_DB $STACKSYNC_USER -c "INSERT INTO workspace_user(workspace_id, user_id, workspace_name, parent_item_id) VALUES ((SELECT id FROM workspace WHERE swift_container='$1'), (SELECT id FROM user1 WHERE name='$1'), 'default', NULL);"
