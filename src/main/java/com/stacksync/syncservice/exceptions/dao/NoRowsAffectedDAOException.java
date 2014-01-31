package com.stacksync.syncservice.exceptions.dao;

import com.stacksync.syncservice.db.DAOError;

public class NoRowsAffectedDAOException extends DAOException {

	private static final long serialVersionUID = 356684827372558709L;

    public NoRowsAffectedDAOException(DAOError error) {
        super(error);
    }
    
    public NoRowsAffectedDAOException(Exception e, DAOError error) {
        super(e, error);
    }

    public NoRowsAffectedDAOException(String message) {
        super(message);
    }

    public NoRowsAffectedDAOException(Throwable cause) {
        super(cause);
    }

    public NoRowsAffectedDAOException(String message, Throwable cause) {
        super(message, cause);
    }

}