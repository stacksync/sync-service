package com.stacksync.syncservice.test.xmlrpc;

import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class ApiPutMetadataFolder {

	//TODO: probar con el mismo nombre de un archivo
	//TODO: corregir path cuando la carpeta esta dentro de otra.
	public static void main(String[] args) throws Exception {

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		config.setEnabledForExtensions(true);
		config.setServerURL(new URL("http://127.0.0.1:" + Constants.XMLRPC_PORT));
		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);

		Object[] params = new Object[] { Constants.USER, Constants.REQUESTID, "hola5", null};
																			// strFolderName, strParentId

		long startTotal = System.currentTimeMillis();
		String strResponse = (String) client.execute("XmlRpcSyncHandler.putMetadataFolder", params);

		System.out.println("Response --> " + Constants.PrettyPrintJson(strResponse));

		long totalTime = System.currentTimeMillis() - startTotal;
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
