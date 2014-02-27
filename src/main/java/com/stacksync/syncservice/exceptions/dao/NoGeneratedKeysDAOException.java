package com.stacksync.syncservice.exceptions.dao;

import com.stacksync.syncservice.db.DAOError;

public class NoGeneratedKeysDAOException extends DAOException {

	private static final long serialVersionUID = -2938093497450050090L;

	public NoGeneratedKeysDAOException(DAOError error) {
        super(error);
    }
    
    public NoGeneratedKeysDAOException(Exception e, DAOError error) {
        super(e, error);
    }

    public NoGeneratedKeysDAOException(String message) {
        super(message);
    }

    public NoGeneratedKeysDAOException(Throwable cause) {
        super(cause);
    }

    public NoGeneratedKeysDAOException(String message, Throwable cause) {
        super(message, cause);
    }

}