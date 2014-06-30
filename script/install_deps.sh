#!/usr/bin/env bash
if [[ $UID -ne 0 ]]; then
	echo "$0 must be run as root"
	exit 1
fi

### INSTALL DEPENDENCIES
apt-get -y install ruby python-keystoneclient curl 
