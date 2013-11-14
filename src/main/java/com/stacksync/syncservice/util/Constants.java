package com.stacksync.syncservice.util;

import omq.common.util.ParameterQueue;

public class Constants {

	/* PROPERTIES */
	public static final String PROP_DATASOURCE = "datasource";
	public static final String DEFAULT_CONFIG_FILE = "config.properties";

	// ObjectMQ
	public static final String PROP_OMQ_HOST = ParameterQueue.RABBIT_HOST;
	public static final String PROP_OMQ_PORT = ParameterQueue.RABBIT_PORT;
	public static final String PROP_OMQ_EXCHANGE = ParameterQueue.RPC_EXCHANGE;
	public static final String PROP_OMQ_THREADS = ParameterQueue.NUM_THREADS;
	public static final String PROP_OMQ_USER = ParameterQueue.USER_NAME;
	public static final String PROP_OMQ_PASSWORD = ParameterQueue.USER_PASS;

	// PostgreSQL
	public static final String PROP_POSTGRESQL_HOST = "postgresql.host";
	public static final String PROP_POSTGRESQL_PORT = "postgresql.port";
	public static final String PROP_POSTGRESQL_DATABASE = "postgresql.database";
	public static final String PROP_POSTGRESQL_USERNAME = "postgresql.user";
	public static final String PROP_POSTGRESQL_PASSWORD = "postgresql.password";
	public static final String PROP_POSTGRESQL_INITIAL_CONNS = "postgresql.initial_cons";
	public static final String PROP_POSTGRESQL_MAX_CONNS = "postgresql.max_cons";

	/* GENERAL */
	public static final String DEFAULT_DATASOURCE = "postgresql";

	/* QUEUE */
	public static final String DEFAULT_OMQ_HOST = "localhost";
	public static final Integer DEFAULT_OMQ_PORT = 5672;
	public static final String DEFAULT_OMQ_EXCHANGE = "rpc_global_exchange";
	public static final String DEFAULT_OMQ_USER = "guest";
	public static final String DEFAULT_OMQ_PASSWORD = "guest";
	public static final String DEFAULT_OMQ_THREADS = "4";

	/* POSTGRESQL */
	public static final String DEFAULT_POSTGRESQL_HOST = "localhost";
	public static final String DEFAULT_POSTGRESQL_PORT = "5432";
	public static final String DEFAULT_POSTGRESQL_DATABASE = "stacksync";
	public static final String DEFAULT_POSTGRESQL_DRIVER = "org.postgresql.Driver";
	public static final String DEFAULT_POSTGRESQL_USERNAME = "stacksync";
	public static final String DEFAULT_POSTGRESQL_PASSWORD = "stacksync";
	public static final String DEFAULT_POSTGRESQL_INITIAL_CONNS = "1";
	public static final String DEFAULT_POSTGRESQL_MAX_CONNS = "100";

	/* XMLRPC */
	public static final Integer XMLRPC_PORT = 61234;
}
