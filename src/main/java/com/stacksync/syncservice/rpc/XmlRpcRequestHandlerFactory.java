package com.stacksync.syncservice.rpc;

import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestProcessorFactory;

public class XmlRpcRequestHandlerFactory implements RequestProcessorFactoryFactory, RequestProcessorFactory {

	private Map<String, Object> handlerMap = new HashMap<String, Object>();

	public void setHandler(String name, Object handler) {
		this.handlerMap.put(name, handler);
	}

	public Object getHandler(String name) {
		return this.handlerMap.get(name);
	}

	public RequestProcessorFactory getRequestProcessorFactory(@SuppressWarnings("rawtypes") Class arg0)
			throws XmlRpcException {
		return this;
	}

	public Object getRequestProcessor(XmlRpcRequest request) throws XmlRpcException {

		String handlerName = request.getMethodName().substring(0, request.getMethodName().lastIndexOf("."));

		if (!handlerMap.containsKey(handlerName)) {
			throw new XmlRpcException("Unknown handler: " + handlerName);
		}

		return handlerMap.get(handlerName);
	}
}
