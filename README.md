StackSync Synchronization service
=================================


**Table of Contents**

- [Introduction](#introduction)
- [Architecture](#architecture)
- [Synchronization service](#synchronization-service)
- [Requirements](#requirements)
- [Setup](#setup)
    - [Database initialization](database-initialization)
    - [Create new users](#create-new-users)
- [Compilation](#compilation)
- [Configuration](#configuration)
- [Execution](#execution)
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
  <img width="500" src="https://raw.github.com/stacksync/desktop/master/res/stacksync-architecture.png">
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
* RabbitMQ

# Setup

## Database initialization

In order to initialize the database we need to create the database and the user and execute the script “setup_db.sql” located in "src/main/resources".

First, enter in a Postgres command line mode:

    $ sudo -u postgres psql

Execute the commands below to create a user and the database. The database must be called stacksync:

    postgres=# create database stacksync;
    postgres=# create user stacksync_user with password 'mysecretpwd';
    postgres=# grant all privileges on database stacksync to stacksync_user;
    postgres=# \q

Enter to the database with the user role created. Note that the first parameter is the host, the second is the database name and the last one is the username:

    $ psql -h localhost stacksync stacksync_user

Now run execute the script.

    postgres=# \i ./setup_db.sql
    postgres=# \q


## Create new users

Go to the "script" folder inside the SyncService project and run the following command to install the necessary tools to create users. It will install Ruby and some dependencies.

    $ sudo ./install.sh

Now run the following scripts to add a new user and a new workspace associated to the user. The username must be the same as the one in the storage backend.

    $ ./postgres/adduser.rb -i <USER> -n <USER> -q 1
    $ ./postgres/addworkspace.rb -i <USER> -p /



# Compilation

We just need to assemble the project into a JAR using Maven:

    $ mvn assembly:assembly

This will generate a "target" folder containing a JAR file called "syncservice-X.X-jar-with-dependencies.jar"

> **NOTE**: if you get an error (BUILD FAILURE), cleaning your local Maven repository may fix the problem.

    $ rm -rf ~/.m2/repository/*


# Configuration

To generate the properties file you can just run the JAR with the argument <code>--dump-config</code> and redirect the output to a new file:

    $ java -jar syncservice-X.X-jar-with-dependencies.jar --dump-config > config.properties


# Execution

Run the following command specifying the location of your configuration file.

    $ java -jar syncservice-X.X-jar-with-dependencies.jar --config config.properties

Other parameters:

- **Help (-h, --help)**: Shows the different execution options.
- **Config (-c, --config)**: To provide a configuration file.
- **Version (-v, --version)**: Prints the application version.
- **Dump config (--dump-config)**: Dumps an example of configuration file, you can redirect the output to a new file to edit the configuration.


# Issue Tracking
We use the GitHub issue tracking.

# Licensing
StackSync is licensed under the GPLv3. Check [LICENSE](LICENSE) for the latest
licensing information.

# Contact
Visit www.stacksync.com for contact information.
