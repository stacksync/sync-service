package com.stacksync.syncservice.test.xmlrpc;

import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class ApiPutMetadataFile {

		
	//TODO: sube de version siempre aunque sea igual
	//TODO: overwrite
	public static void main(String[] args) throws Exception {

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setEnabledForExtensions(true);
		config.setServerURL(new URL("http://127.0.0.1:" + Constants.XMLRPC_PORT));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);

		String[] chunks = new String[] {"1111", "2222", "3333"};
		Object[] params = new Object[] { Constants.USER, Constants.REQUESTID, "test.txt", "", "", "111111", "10", "Text", chunks};
																			// strFileName, strParentId, strOverwrite, strChecksum, strFileSize,
																			// strMimetype, strChunks

		long startTotal = System.currentTimeMillis();
		String strResponse = (String) client.execute("XmlRpcSyncHandler.putMetadataFile", params);

		System.out.println("Response --> " + Constants.PrettyPrintJson(strResponse));

		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
