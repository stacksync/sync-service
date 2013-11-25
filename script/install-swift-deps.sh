#!/usr/bin/env bash
if [[ $UID -ne 0 ]]; then
        echo "$0 must be run as root"
        exit 1
fi

apt-get -y install python-keystoneclient curl
