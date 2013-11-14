package com.stacksync.syncservice.test.xmlrpc;

import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class ApiRestoreMetadata {

	public static void main(String[] args) throws Exception {

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setEnabledForExtensions(true);
		config.setServerURL(new URL("http://127.0.0.1:" + Constants.XMLRPC_PORT));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);

		Object[] params = new Object[] { Constants.USER, Constants.REQUESTID, "887611628764801051", "2"};
																			// strFileId, strVersion

		long startTotal = System.currentTimeMillis();
		String strResponse = (String) client.execute("XmlRpcSyncHandler.restoreMetadata", params);

		System.out.println("Response --> " + Constants.PrettyPrintJson(strResponse));

		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
