package com.stacksync.syncservice.rpc;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class XmlRpcSyncServer {

	private int port;
	private WebServer webServer = null;
	private PropertyHandlerMapping phm = null;
	private XmlRpcRequestHandlerFactory handler = null;
	private XmlRpcServer xmlRpcServer = null;

	public XmlRpcSyncServer(int port) throws Exception {
		this.port = port;
		// bind
		this.webServer = new WebServer(this.port);
		this.xmlRpcServer = this.webServer.getXmlRpcServer();
		this.handler = new XmlRpcRequestHandlerFactory();

		this.phm = new PropertyHandlerMapping();
		this.phm.setRequestProcessorFactoryFactory(this.handler);

	}

	public void addHandler(String name, Object requestHandler) throws Exception {

		this.handler.setHandler(name, requestHandler);
		this.phm.addHandler(name, requestHandler.getClass());

	}

	public void serve_forever() throws Exception {
		// init
		this.xmlRpcServer.setHandlerMapping(phm);
		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		serverConfig.setEnabledForExtensions(true);
		this.webServer.start();
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public WebServer getWebServer() {
		return webServer;
	}

	public void setWebServer(WebServer webServer) {
		this.webServer = webServer;
	}

	public PropertyHandlerMapping getPhm() {
		return phm;
	}

	public void setPhm(PropertyHandlerMapping phm) {
		this.phm = phm;
	}

	public XmlRpcRequestHandlerFactory getHandler() {
		return handler;
	}

	public void setHandler(XmlRpcRequestHandlerFactory handler) {
		this.handler = handler;
	}

	public XmlRpcServer getXmlRpcServer() {
		return xmlRpcServer;
	}

	public void setXmlRpcServer(XmlRpcServer xmlRpcServer) {
		this.xmlRpcServer = xmlRpcServer;
	}

}
