package com.isteer.vms.response;

public enum ResponseCode {

	NEW_COMPUTER_CREATED(1001, "New computer created successfully"),
	COMPUTER_UPDATED(1002, "Computer updated successfully"),
	APPLICATION_UPDATED(1003, "Application data updated successfully"),
	NO_CHANGES_MADE(1004, "No changes made to the computer"),
	COMPUTER_AND_APPLICATION_UPDATED(1005, "Computer and application updated successfully"),
	NO_DATA_FOUND(1006, "No data found for the given request"),
	
	COMPUTER_DELETED(9001, "Cannot update computer as it is deleted. Please restore it first."),
	COMPUTER_INACTIVE(9002, "Cannot update computer as it is inactive. Please activate it first."),
	APPLICATION_ERROR(9003, "Error while processing application data. Please check the input data"),
	DEFAULT_ERROR(9004, "Cannot process request. Please try again later."),
	METHOD_ARGUMENT_NOT_VALID(1005, "Invalid input data. Please check the data and try again."),
	INTERNAL_ERROR(9999, "Internal server error. Please try again later.");
	
	private final int code;
	private final String message;
	
	ResponseCode(int code, String message) {
		this.code = code;
		this.message = message;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getMessage() {
		return message;
	}
}
