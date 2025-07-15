package com.isteer.vms.exception;

import com.isteer.vms.enums.Message;

public class BusinessException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final Message error;
	
	public BusinessException(Message error) {
		super(error.getMessageKey());
		this.error = error;
	}
	
	public Message getError() {
		return error;
	}
}
