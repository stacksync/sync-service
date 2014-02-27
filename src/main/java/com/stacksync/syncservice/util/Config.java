package com.stacksync.syncservice.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class Config {

	private static Properties properties;
	private static final Logger logger = Logger.getLogger(Config.class.getName());

	public static void loadProperties() throws IOException {
		logger.warn(String.format("Config argument not passed, will use default config file: '%s'",
				Constants.DEFAULT_CONFIG_FILE));

		loadProperties(Constants.DEFAULT_CONFIG_FILE);
	}

	public static void loadProperties(String configFileUrl) throws IOException {

		logger.info(String.format("Loading config file: '%s'", configFileUrl));

		URL log4jResource = Config.class.getResource("/log4j.xml");
		DOMConfigurator.configure(log4jResource);

		properties = new Properties();

		properties.load(new FileInputStream(configFileUrl));

		validateProperties();

		logger.info("Configuration file loaded");

		displayConfiguration();
	}

	public static Properties getProperties() {
		return properties;
	}

	private static void validateProperties() {
		checkProperty(Constants.PROP_DATASOURCE, Constants.DEFAULT_DATASOURCE);

		// ObjectMQ
		checkProperty(Constants.PROP_OMQ_HOST, Constants.DEFAULT_OMQ_HOST);
		checkProperty(Constants.PROP_OMQ_PORT, Constants.DEFAULT_OMQ_PORT);
		checkProperty(Constants.PROP_OMQ_THREADS, Constants.DEFAULT_OMQ_THREADS);
		checkProperty(Constants.PROP_OMQ_EXCHANGE, Constants.DEFAULT_OMQ_EXCHANGE);
		checkProperty(Constants.PROP_OMQ_USER, Constants.DEFAULT_OMQ_USER);
		checkProperty(Constants.PROP_OMQ_PASSWORD, Constants.DEFAULT_OMQ_PASSWORD);

		// Database
		checkProperty(Constants.PROP_POSTGRESQL_HOST, Constants.DEFAULT_POSTGRESQL_HOST);
		checkProperty(Constants.PROP_POSTGRESQL_PORT, Constants.DEFAULT_POSTGRESQL_PORT);
		checkProperty(Constants.PROP_POSTGRESQL_DATABASE, Constants.DEFAULT_POSTGRESQL_DATABASE);
		checkProperty(Constants.PROP_POSTGRESQL_USERNAME, Constants.DEFAULT_POSTGRESQL_USERNAME);
		checkProperty(Constants.PROP_POSTGRESQL_PASSWORD, Constants.DEFAULT_POSTGRESQL_PASSWORD);
	}

	private static void checkProperty(String key, Object defaultValue) {
		if (!properties.containsKey(key)) {
			logger.warn(String.format("Property '%s' not found, using default value: '%s'", key,
					defaultValue.toString()));
			properties.setProperty(key, defaultValue.toString());
		}
	}

	private static void displayConfiguration() {
		logger.info("Displaying configuration:");

		Enumeration<?> e = properties.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			logger.info(String.format("%s : %s", key, properties.getProperty(key)));
		}
	}

	public static String getDatasource() {
		return properties.getProperty(Constants.PROP_DATASOURCE);
	}

	public static String getOmqHost() {
		return properties.getProperty(Constants.PROP_OMQ_HOST);
	}

	public static Integer getOmqPort() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_OMQ_PORT));
	}

	public static String getOmqExchange() {
		return properties.getProperty(Constants.PROP_OMQ_EXCHANGE);
	}

	public static String getOmqUser() {
		return properties.getProperty(Constants.PROP_OMQ_USER);
	}

	public static String getOmqPassword() {
		return properties.getProperty(Constants.PROP_OMQ_PASSWORD);
	}

	public static Integer getOmqNumThreads() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_OMQ_THREADS, Constants.DEFAULT_OMQ_THREADS));
	}

	/* POSTGRESQL */

	public static String getPostgresqlHost() {
		return properties.getProperty(Constants.PROP_POSTGRESQL_HOST);
	}

	public static Integer getPostgresqlPort() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_POSTGRESQL_PORT));
	}

	public static String getPostgresqlDatabase() {
		return properties.getProperty(Constants.PROP_POSTGRESQL_DATABASE);
	}

	public static String getPostgresqlUsername() {
		return properties.getProperty(Constants.PROP_POSTGRESQL_USERNAME);
	}

	public static String getPostgresqlPassword() {
		return properties.getProperty(Constants.PROP_POSTGRESQL_PASSWORD);
	}

	public static Integer getPostgresqlInitialConns() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_POSTGRESQL_INITIAL_CONNS,
				Constants.DEFAULT_POSTGRESQL_INITIAL_CONNS));
	}

	public static Integer getPostgresqlMaxConns() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_POSTGRESQL_MAX_CONNS,
				Constants.DEFAULT_POSTGRESQL_MAX_CONNS));
	}

	/* SWIFT */

	public static String getSwiftHost() {
		return properties.getProperty(Constants.PROP_SWIFT_HOST, Constants.DEFAULT_SWIFT_HOST);
	}
	
	public static Integer getSwiftPort() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_SWIFT_PORT,
				Constants.DEFAULT_SWIFT_PORT));
	}

	public static String getSwiftProtocol() {
		return properties
				.getProperty(Constants.PROP_SWIFT_PROTOCOL, Constants.DEFAULT_SWIFT_PROTOCOL);
	}
	
	
	public static String getSwiftKeystoneHost() {
		return properties.getProperty(Constants.PROP_SWIFT_KEYSTONE_HOST, Constants.DEFAULT_SWIFT_KEYSTONE_HOST);
	}

	public static Integer getSwiftKeystoneAdminPort() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_SWIFT_KEYSTONE_ADMIN_PORT,
				Constants.DEFAULT_SWIFT_KEYSTONE_ADMIN_PORT));
	}
	
	public static Integer getSwiftKeystonePort() {
		return Integer.parseInt(properties.getProperty(Constants.PROP_SWIFT_KEYSTONE_PORT,
				Constants.DEFAULT_SWIFT_KEYSTONE_PORT));
	}

	public static String getSwiftKeystoneProtocol() {
		return properties
				.getProperty(Constants.PROP_SWIFT_KEYSTONE_PROTOCOL, Constants.DEFAULT_SWIFT_KEYSTONE_PROTOCOL);
	}

	public static String getSwiftTenant() {
		return properties.getProperty(Constants.PROP_SWIFT_TENANT, Constants.DEFAULT_SWIFT_TENANT);
	}

	public static String getSwiftUser() {
		return properties.getProperty(Constants.PROP_SWIFT_USER, Constants.DEFAULT_SWIFT_USER);
	}

	public static String getSwiftPassword() {
		return properties.getProperty(Constants.PROP_SWIFT_PASSWORD, Constants.DEFAULT_SWIFT_PASSWORD);
	}
	
	public static String getSwiftAuthUrl(){
		String authUrl = String.format("%s://%s:%s/v2.0/tokens", getSwiftKeystoneProtocol(), getSwiftKeystoneHost(), getSwiftKeystonePort());
		return authUrl;
	}
	
	public static String getSwiftUrl(){
		String url = String.format("%s://%s:%s/v1", getSwiftProtocol(), getSwiftHost(), getSwiftPort());
		return url;
	}
}
