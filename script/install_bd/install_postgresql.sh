#!/bin/bash

# check user is sudo
if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

# install puppet
apt-get -y install puppet
puppet module install puppetlabs/postgresql
sleep 10

# install postgresql
puppet apply install_postgresql.pp
sleep 10
service postgresql restart

# create users
puppet apply create_users_postgresql.pp
PGPASSWORD='milax' psql -h localhost stacksync postgres -c "CREATE EXTENSION \"uuid-ossp\";"
PGPASSWORD='stacksync_pass' psql -h localhost stacksync stacksync_user -f setup_db.sql


