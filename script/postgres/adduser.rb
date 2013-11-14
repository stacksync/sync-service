#!/usr/bin/ruby
require 'rubygems'
require 'pg'
require 'json'
require 'choice'
require 'json'


# Parameters (user_id, email(opcional), nickname, storage_url, quota -> limit)
Choice.options do
  header 'Application options:'

  separator 'Required:'
  option :user_id, :required => true do
	short '-i'
	long '--id=USER_ID'
	desc 'The user identifier.'
  end
  
  option :nickname, :required => true do
	short '-n'
	long '--nickname=USER_NICKNAME'
	desc 'The user nickname.'
  end
  
  option :quota_limit, :required => true do
	short '-q'
	long '--quota=USER_QUOTA'
	desc 'The user quota limit.'
  end

  separator 'Optional:'
  option :email do
	short '-e'
	long '--email=USER_EMAIL'
	desc 'The user email.'
	default ''
  end

  separator 'Common:'
  option :version do
	short '-v'
	long '--version'
	desc 'Show version.'
	action do
	  puts 'Riak add user version 1.0'
	  exit
	end
  end
end


if !File.exist?("./settings.conf")
	abort("The file settings doesn't exist.")
end

configuration = JSON[IO.read("./settings.conf")]
# Create a client interface
dbh = PG.connect(configuration['ip-db'], 5432, '', '', configuration['postgres-db'], configuration['postgres-db-user'], configuration['postgres-db-password'])

## Insert user in user table
dbh.prepare('exist_user', 'SELECT * FROM public.user1 WHERE cloud_id = $1;')
existUser = dbh.exec_prepared('exist_user', [Choice.choices.user_id])

if existUser.num_tuples() == 0
	dbh.prepare('insert_user', 'INSERT INTO user1 (email, name, cloud_id, quota_limit, quota_used) VALUES ($1, $2, $3, $4, $5);')
	dbh.exec_prepared('insert_user', [Choice.choices.email, Choice.choices.nickname, Choice.choices.user_id, Choice.choices.quota_limit, 0])
else
	userDbId = existUser[0]['id']
	
	dbh.prepare('update_user', 'UPDATE user1 SET email = $1, name = $2, cloud_id = $3, quota_limit = $4, quota_used = $5 WHERE id = $6;')
	dbh.exec_prepared('update_user', [Choice.choices.email, Choice.choices.nickname, Choice.choices.user_id, Choice.choices.quota_limit, 0, userDbId])
end

existUser = dbh.exec_prepared('exist_user', [Choice.choices.user_id])

## Don't need to initialize the table device
puts "user1 (email, name, cloud_id, quota_limit, quota_used) VALUES (" + existUser[0]['id'] + "," + existUser[0]['email'] + ", " + existUser[0]['name'] + ", " + existUser[0]['cloud_id'] + ", " + 
					 existUser[0]['quota_limit'] + ", " + existUser[0]['quota_used'] + ")"

dbh.close()
