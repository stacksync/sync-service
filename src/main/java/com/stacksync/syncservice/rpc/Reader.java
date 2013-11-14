package com.stacksync.syncservice.rpc;

import com.stacksync.syncservice.exceptions.DAOConfigurationException;
import com.stacksync.syncservice.exceptions.InvalidReader;
import com.stacksync.syncservice.rpc.parser.IParser;

public class Reader {

	/**
	 * @throws DAOConfigurationException
	 * 
	 */
	public static IParser getInstance(String className) throws InvalidReader {
		try {
			if (className == null || className.isEmpty()) {
				throw new ClassNotFoundException("Class name is null or empty.");
			}

			IParser instance = (IParser) Class.forName(className).newInstance();
			return instance;
		} catch (Exception ex) {
			throw new InvalidReader(ex.getMessage(), ex);
		}
	}

}
