class { 'postgresql::server': }

postgresql::server::role { 'stacksync_user':
  	password_hash => postgresql_password('stacksync_user', 'stacksync_pass'),
}

postgresql::server::database_grant { 'stacksync':
  	privilege => 'ALL',
  	db        => 'stacksync',
  	role      => 'stacksync_user',
}

# exec { "psql-h localhost stacksync postgres -c \"CREATE EXTENSION \"uuid-ossp\";\"":
# user   => "postgres"
# }
