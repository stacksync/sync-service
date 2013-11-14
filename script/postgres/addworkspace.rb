#!/usr/bin/ruby
require 'rubygems'
require 'pg'
require 'json'
require 'choice'


# Parameters (user_id, path)
Choice.options do
  header 'Application options:'

  separator 'Required:'
  option :user_id, :required => true do
	short '-i'
	long '--id=USER_ID'
	desc 'The user identifier.'
  end
  
  option :path, :required => true do
	short '-p'
	long '--path=PATH'
	desc 'The workspace path.'
  end
  
  separator 'Optional:'  
  option :ip_database do
	short '-ip_db'
	long '--ip_database=ip_database'
	desc 'The database ip.'
	default '127.0.0.1'
  end   
  
  
  separator 'Common:'
  option :version do
	short '-v'
	long '--version'
	desc 'Show version.'
	action do
	  puts 'Riak add workspace version 1.0'
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


## Insert workspace in workspace table
workspace_id = Choice.choices.user_id + Choice.choices.path


## check exist user
dbh.prepare('exist_user', 'select * from public.user1 where cloud_id = $1;')
existUser = dbh.exec_prepared('exist_user', [Choice.choices.user_id])

if existUser.num_tuples() > 0
	userDbId = existUser[0]['id']
else
	abort("Error user doesn't exist in database.")
end


dbh.prepare('exist_workspace', 'select * from public.workspace where client_workspace_name = $1;')
existWorkspace = dbh.exec_prepared('exist_workspace', [workspace_id])

if existWorkspace.num_tuples() == 0
	dbh.prepare('insert_workspace', 'INSERT INTO workspace (client_workspace_name, latest_revision, owner_id) VALUES ($1, $2, $3);')
	insertUser = dbh.exec_prepared('insert_workspace', [workspace_id, 0, userDbId])
	
	existWorkspace = dbh.exec_prepared('exist_workspace', [workspace_id])
#else 
	#update(todo)!!!
end

workspaceId = existWorkspace[0]['id']
puts "workspace (client_workspace_name, latest_revision, owner_id) VALUES (" + existWorkspace[0]['id'] + ", " + existWorkspace[0]['client_workspace_name'] + ", " + existWorkspace[0]['latest_revision'] + ", " + existWorkspace[0]['owner_id'] + ")"

dbh.prepare('exist_workspace_user', 'select * from public.workspace_user where workspace_id = $1 and user_id = $2;')
existWorkspaceUser = dbh.exec_prepared('exist_workspace_user', [workspaceId, userDbId])


if existWorkspaceUser.num_tuples() == 0	
	dbh.prepare('insert_workspace_user', 'INSERT INTO workspace_user (workspace_id, user_id, client_workspace_path) VALUES ($1, $2, $3);')
	insertUser = dbh.exec_prepared('insert_workspace_user', [workspaceId, userDbId, "/"])
	
	existWorkspaceUser = dbh.exec_prepared('exist_workspace_user', [workspaceId, userDbId])
#else 
	#update(todo)!!!
end

puts "workspace_user (workspace_id, user_id, client_workspace_path) VALUES (" + existWorkspaceUser[0]['workspace_id'] + ", " + existWorkspaceUser[0]['user_id'] + ", " + existWorkspaceUser[0]['client_workspace_path'] + ")"

dbh.prepare('delete_objects_user', 'DELETE FROM object where workspace_id = $1;')
dbh.exec_prepared('delete_objects_user', [workspaceId])	
puts "Delete Objects!"

dbh.close()
