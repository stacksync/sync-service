package com.stacksync.syncservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import omq.common.broker.Broker;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.exceptions.DAOConfigurationException;
import com.stacksync.syncservice.omq.ISyncService;
import com.stacksync.syncservice.omq.SyncServiceImp;
import com.stacksync.syncservice.rpc.XmlRpcSyncHandler;
import com.stacksync.syncservice.rpc.XmlRpcSyncServer;
import com.stacksync.syncservice.util.Config;
import com.stacksync.syncservice.util.Constants;

public class SyncService {

	private static final Logger logger = Logger.getLogger(SyncService.class.getName());
	private static XmlRpcSyncServer xmlRpcServer = null;
	private static Broker broker = null;
	private static SyncServiceImp syncService = null;
	private static ConnectionPool pool = null;

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException {

		Options options = new Options();
		options.addOption("h", "help", false, "print this message");
		options.addOption("c", "config", true, "path to the configuration file");
		options.addOption("V", "version", false, "print application version");
		Option dump = OptionBuilder.withLongOpt("dump-config").hasArg(false)
				.withDescription("dumps an example of configuration file, you can redirect the output to a new file to edit the configuration").create();
		options.addOption(dump);

		// create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("sync-service [OPTION]...",
						"Runs the StackSync SyncService with the options specified in the config file.\nAvailable options:", options, "");
				System.exit(0);
			}

			if (line.hasOption("version")) {
				String version = SyncService.getVersion();
				System.out.println(String.format("StackSync SyncService v%s", version));
				System.exit(0);
			}

			if (line.hasOption("dump-config")) {
				InputStream exampleStream = Config.class.getResourceAsStream("/example.properties");

				StringWriter writer = new StringWriter();
				IOUtils.copy(exampleStream, writer);
				String output = writer.toString();
				exampleStream.close();

				System.out.print(output);
				System.exit(0);
			}

			if (line.hasOption("config")) {
				String configFileUrl = line.getOptionValue("config");

				File file = new File(configFileUrl);
				if (!file.exists()) {
					System.err.println("ERROR: '" + configFileUrl + "' file not found");
					System.exit(2);
				}

				Config.loadProperties(configFileUrl);

			} else {
				Config.loadProperties();
			}

		} catch (ParseException exp) {
			// oops, something went wrong
			logger.error("Parsing failed. Reason: " + exp.getMessage());
			System.exit(1);
		} catch (IOException e) {
			logger.error("IOException. Reason: " + e.getMessage());
			System.exit(7);
		}

		try {

			String datasource = Config.getDatasource();
			pool = ConnectionPoolFactory.getConnectionPool(datasource);

			// it will try to connect to the DB, throws exception if not
			// possible.
			Connection conn = pool.getConnection();
			conn.close();

			logger.info("Connection to database succeded");
		} catch (DAOConfigurationException e) {
			logger.error("Connection to database failed. Reason: " + e.getMessage());
			System.exit(3);
		} catch (SQLException e) {
			logger.error("Connection to database failed. Reason: " + e.getMessage());
			System.exit(4);
		}

		logger.info("Initializing the SyncService core");
		try {
			broker = new Broker(Config.getProperties());
			syncService = new SyncServiceImp(broker, pool);
			broker.bind(ISyncService.class.getSimpleName(), syncService);
			logger.info("Middleware initialization succeded");
		} catch (Exception e) {
			logger.error("Could not initialize ObjectMQ. Error: " + e.toString());
			System.exit(5);
		}

		logger.info("Initializing XML RPC");
		try {
			launchXmlRpc();
			logger.info("XMLRPC initialization succeded");
		} catch (Exception e) {
			logger.error("Could not initialize XMLRPC. Error: " + e.toString());
			System.exit(6);
		}
	}

	private static void launchXmlRpc() throws Exception {
		xmlRpcServer = new XmlRpcSyncServer(Constants.XMLRPC_PORT);
		xmlRpcServer.addHandler("XmlRpcSyncHandler", new XmlRpcSyncHandler(broker, pool));
		xmlRpcServer.serve_forever();
	}

	private static String getVersion() {
		String path = "/version.properties";
		InputStream stream = Config.class.getResourceAsStream(path);
		if (stream == null) {
			return "UNKNOWN";
		}
		Properties props = new Properties();
		try {
			props.load(stream);
			stream.close();
			return (String) props.get("version");
		} catch (IOException e) {
			return "UNKNOWN";
		}
	}
}
