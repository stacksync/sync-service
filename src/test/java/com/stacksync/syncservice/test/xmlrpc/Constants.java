package com.stacksync.syncservice.test.xmlrpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Constants {
	public static String USER = "AUTH_6d3b65697d5c48d5aaffbb430c9dbe6a";
	public static String WORKSPACEID = "AUTH_6d3b65697d5c48d5aaffbb430c9dbe6a/";
	public static String REQUESTID = "TestXmlRpc";
	public static String DEVICENAME = "Test-Device";
	public static Integer XMLRPC_PORT = com.stacksync.syncservice.util.Constants.XMLRPC_PORT;
	
	public static String PrettyPrintJson(String uglyJSONString){
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(uglyJSONString);
		String prettyJsonString = gson.toJson(je);
		
		return prettyJsonString;
	}
}
