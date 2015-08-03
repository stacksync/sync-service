StackSync Synchronization service
=================================


**Table of Contents**

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Synchronization service](#synchronization-service)
- [Requirements](#requirements)
- [Setup](#setup)
    - [Database initialization](#database-initialization)
    - [Create admin user](#create-admin-user)
    - [Create new users](#create-new-users)
- [Compilation](#compilation)
- [Installation](#installation)
- [Issue Tracking](#issue-tracking)
- [Licensing](#licensing)
- [Contact](#contact)


# Introduction

StackSync (<http://stacksync.com>) is a scalable open source Personal Cloud
that implements the basic components to create a synchronization tool.


# Architecture

In general terms, StackSync can be divided into three main blocks: clients
(desktop and mobile), synchronization service (SyncService) and storage
service (Swift, Amazon S3, FTP...). An overview of the architecture
with the main components and their interaction is shown in the following image.

<p align="center">
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/resources/res/stacksync-architecture.png">
</p>

The StackSync client and the SyncService interact through the communication
middleware called ObjectMQ. The sync service interacts with the metadata
database. The StackSync client directly interacts with the storage back-end
to upload and download files.

As storage back-end we are using OpenStack Swift, an open source cloud storage
software where you can store and retrieve lots of data in virtual containers.
It's based on the Cloud Files offering from Rackspace. But it is also possible
to use other storage back-ends, such as a FTP server or S3.


# Synchronization service

The SyncService is in charge of managing the metadata in order to achieve
data synchronization. Desktop clients communicate with the SyncService for two main
reasons: to obtain the changes occurred when they were offline; and to commit new versions
of a files.

When a client connects to the system, the first thing it does is asking the SyncService
for changes that were made during the offline time period. This is very common situation
for users working with two different computers, e.g., home and work. Assuming the home
computer is off, if the user modifies some files while at work, the home computer will not
realize about these changes until the user turns on the computer. At this time, the client will
ask the SyncService and update to apply the changes made at work.

When the server receives a commit operation of a file, it must first check that the meta-
data received is consistent. If the metadata is correct, it proceeds to save it to a database.
Afterwards, a notification is sent to all devices owned by the user reporting the file update.
This provides StackSync with real-time in all devices as they receive notifications of updates
and are always synchronized. However, if the metadata is incorrect, an notification is sent
to the client to fix the error.


# Requirements

* Java 1.6 or newer
* Maven 2
* PostgreSQL 9
* RabbitMQ 3.2.X (Version 3.3.X is not compatible)

# Setup

## Database initialization

In order to initialize the database we need to create the database and the user and execute the script “setup_db.sql” located in "src/main/resources".

First, enter in a Postgres command line mode:

    $ sudo -u postgres psql

Execute the commands below to create a user and the database. The database must be called stacksync:

    postgres=# create database stacksync;
    postgres=# create user stacksync_user with password 'mysecretpwd';
    postgres=# grant all privileges on database db_name to db_user;
    postgres=# \connect db_name
    postgres=# CREATE EXTENSION "uuid-ossp";
    postgres=# \q

Enter to the database with the user role created. Note that the first parameter is the host, the second is the database name and the last one is the username:

    $ psql -h localhost db_name db_user

Now run execute the script.

    postgres=# \i ./setup_db.sql
    postgres=# \q

## Create admin user

In order to manage StackSync users in Swift (create containers, set ACLs...) it is necessary to create an admin user in Swift.

First of all, create a tenant for StackSync:

    $ keystone tenant-create stacksync
    
After that, create the admin user under this tenant:

    $ keystone user-create --name stacksync_admin --tenant stacksync --pass "secr3te"
    
Finally, assign the admin role to the stacksync_admin user:

    $ keystone user-role-add --user stacksync_admin --role admin --tenant stacksync

## Create new users

Go to the web manager project:
https://github.com/stacksync/manager


# Compilation

We just need to assemble the project into a JAR using Maven:

    $ mvn assembly:assembly

This will generate a "target" folder containing a JAR file called "syncservice-X.X-jar-with-dependencies.jar"

> **NOTE**: if you get an error (BUILD FAILURE), cleaning your local Maven repository may fix the problem.

    $ rm -rf ~/.m2/repository/*

# Create deb package

Under the folder [packaging/debian](packaging/debian) there is the Makefile to create the deb file.

    $ cd packaging/debian
    $ make compile
    $ make package

# Installation
First, install the deb package:
    
    $ sudo dpkg -i stacksync-server_X.X.X_all.deb
    
The StackSync Server has a dependency with the JSVC library, if you experience any problem while installing run the following command:

    $ sudo apt-get -f install
    
Once the server is installed, you must modify the configuration file to connect with the database, the messaging middleware, and OpenStack Swift.

You need to specify a Keystone user capable of creating users and set up ACL on containers on the specific container configured in the file.

    /etc/stacksync-server/stacksync-server.conf
    
The init script assumes that you have a "JAVA\_HOME" environment variable set up, if not, it will execute the java located in “/usr/lib/jvm/default-java”. You can change the Java VM by setting up the “JAVA\_HOME” environment or by modifying the script in:

    /etc/init.d/stacksync-server
    
Once configured, just run the server.

    $ sudo service stacksync-server start
    
If something went wrong, you can check the standard and error log files located in:

    /var/log/stacksync-server/

# Issue Tracking
We use the GitHub issue tracking.

# Licensing
StackSync is licensed under the Apache 2.0. Check [LICENSE](LICENSE) for the latest
licensing information.

# Contact
Visit www.stacksync.com for contact information.
