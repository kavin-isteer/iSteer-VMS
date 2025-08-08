package com.isteer.vms.exception;

public class BusinessException extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int statusCode;
	
	public BusinessException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
}
