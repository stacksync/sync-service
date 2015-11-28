package com.stacksync.syncservice.exceptions.dao;

import com.stacksync.syncservice.db.DAOError;

import java.io.Serializable;

/**
 * This class represents a generic DAO exception. It should wrap any exception on the database level
 * , such as SQLExceptions.
 */
public class DAOException extends Throwable implements Serializable {

	private static final long serialVersionUID = 1L;
	private DAOError error;
	/**
     * Constructs a DAOException with the given detail message.
     * @param message The detail message of the DAOException.
     */
    public DAOException(DAOError error) {
        super(error.getMessage());
        this.error = error;
    }
    
    public DAOException(Exception e, DAOError error) {
        super(e);
        this.error = error;
    }

    public DAOException(String message) {
        super(message);
    }
    
    public DAOError getError(){
    	return this.error;
    }
    /**
     * Constructs a DAOException with the given root cause.
     * @param cause The root cause of the DAOException.
     */
    public DAOException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a DAOException with the given detail message and root cause.
     * @param message The detail message of the DAOException.
     * @param cause The root cause of the DAOException.
     */
    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }

}