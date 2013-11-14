#!/usr/bin/env bash
if [[ $UID -ne 0 ]]; then
	echo "$0 must be run as root"
	exit 1
fi

echo ""
echo "============================"
echo "===Installing  deps========="
echo "============================"

apt-get update

### INSTALL DEPENDENCIES

### INSTALL RUBY
apt-get -y install ruby ruby-dev rubygems 
gem install json choice

	### INSTALL POSTGRESQL(TODO!!!)
	apt-get -y install libpq-dev

	gem install pg

echo ""
echo "==================================="
echo "=====Installation has finished====="
echo "==================================="
