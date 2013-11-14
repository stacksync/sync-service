package com.stacksync.syncservice.test.reader;

import org.junit.BeforeClass;
import org.junit.Test;

import com.stacksync.syncservice.rpc.parser.JSONParser;

public class JSONParserTest {

	private static JSONParser reader;
	private static String commitMsgSimple;
	private static byte[] commitMsgByteSimple;
	private static String commitMsgMulti;
	private static byte[] commitMsgByteMulti;
	
	
	@BeforeClass
	public static void initializeData() {
		
		reader = new JSONParser();
		
		commitMsgSimple = "{" +
				"'user':'user1'," +
				"'type':'commit'," +
				"'workspace':'user1/'," +
				"'requestId':'cotes_lab-1361792472697'," +
				"'device':'cotes_lab'," +
				"'metadata':[{" +
					"'rootId':'stacksync'," +
					"'fileId':6191744574108779128," +
					"'version':1," +
					"'parentRootId':''," +
					"'parentFileId':''," +
					"'parentFileVersion':''," +
					"'updated':1361792469869," +
					"'status':'NEW'," +
					"'lastModified':1360830516000," +
					"'checksum':3499525671," +
					"'clientName':'cotes_lab'," +
					"'fileSize':1968," +
					"'folder':'0'," +
					"'name':'pgadmin.log'," +
					"'path':'/'," +
					"'mimetype':'text/plain'," +
					"'chunks':['29ECAA1D936E746D032C1A264A619746C3B5A7E4']}]}";
		
		commitMsgByteSimple = commitMsgSimple.getBytes();
		
		commitMsgMulti = "{" +
				"'user':'user1'," +
				"'type':'commit'," +
				"'workspace':'user1/'," +
				"'requestId':'cotes_lab-1361792472697'," +
				"'device':'cotes_lab'," +
				"'metadata':[{" +
					"'rootId':'stacksync'," +
					"'fileId':6191744574108779128," +
					"'version':1," +
					"'parentRootId':''," +
					"'parentFileId':''," +
					"'parentFileVersion':''," +
					"'updated':1361792469869," +
					"'status':'NEW'," +
					"'lastModified':1360830516000," +
					"'checksum':3499525671," +
					"'clientName':'cotes_lab'," +
					"'fileSize':1968," +
					"'folder':'0'," +
					"'name':'pgadmin.log'," +
					"'path':'/'," +
					"'mimetype':'text/plain'," +
					"'chunks':['29ECAA1D936E746D032C1A264A619746C3B5A7E4']}," +
					"{"+
					"'rootId':'stacksync'," +
					"'fileId':6191744574108779128," +
					"'version':2," +
					"'parentRootId':''," +
					"'parentFileId':''," +
					"'parentFileVersion':''," +
					"'updated':1361792469000," +
					"'status':'CHANGED'," +
					"'lastModified':1360830517000," +
					"'checksum':3499525600," +
					"'clientName':'cotes_lab'," +
					"'fileSize':1900," +
					"'folder':'0'," +
					"'name':'pgadmin.log'," +
					"'path':'/'," +
					"'mimetype':'text/plain'," +
					"'chunks':['29ECAA1D936E746D032C1A264A619746C3B5A000','111CAA1D936E746D032C1A264A619746C3B5A000']}]}";
		
		commitMsgByteMulti = commitMsgMulti.getBytes();
	}
	
	@Test
	public void testCreateResponse() {
		
	}
	
}
