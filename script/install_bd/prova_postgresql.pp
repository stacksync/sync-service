# execute 'apt-get update'
exec { 'apt-update':                  # exec resource named 'apt-update'
  	command => '/usr/bin/apt-get update'  # command this resource will run
}

package { 'postgresql':
	require => Exec['apt-update'],	 # require 'apt-update' before installing
	ensure => installed,
}

package { 'pgadmin3':
	require => Exec['apt-update'],	 # require 'apt-update' before installing
	ensure => installed,
}

class { 'postgresql::server':	
	ip_mask_allow_all_users    => '0.0.0.0/0',
  	listen_addresses           => '*',
	postgres_password          => 'milax',
}

# Install contrib modules
class { 'postgresql::server::contrib':
 	package_ensure => 'present',
}

# Create db named stacksync with user stacksync_user and stacksync_pass as a passwd
postgresql::server::db { 'stacksync':
	user => 'stacksync_user',
	password => postgresql_password('stacksync_user', 'stacksync_pass'),
}

postgresql::server::role { 'stacksync_user':
	password_hash => postgresql_password('stacksync_user', 'stacksync_pass'),
	createdb  => true,
}

# grant all privileges on database stacksync to stacksync_user;
postgresql::server::database_grant { 'asdf':
  privilege => 'ALL',
  db        => 'stacksync',
  role      => 'stacksync_user',
}

# CREATE EXTENSION "uuid-ossp" on stacksync
# more info: https://coderwall.com/p/qwdywg/create-postgresql-extension-with-puppet
exec { "/usr/bin/psql -d stacksync -c 'CREATE EXTENSION uuid-ossp;'":
 	user   => "stacksync_user",
	unless => "/usr/bin/psql -d stacksync -c '\\dx' | grep uuid-ossp",
}
