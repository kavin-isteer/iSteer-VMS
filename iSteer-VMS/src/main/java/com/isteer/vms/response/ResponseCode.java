package com.isteer.vms.response;

public enum ResponseCode {

	NEW_COMPUTER_CREATED(1001, "New computer created successfully"),
	COMPUTER_UPDATED(1002, "Computer updated successfully"),
	APPLICATION_UPDATED(1003, "Application data updated successfully"),
	NO_CHANGES_MADE(1004, "No changes made to the computer"),
	COMPUTER_AND_APPLICATION_UPDATED(1005, "Computer and application updated successfully"),
	NO_DATA_FOUND(1006, "No data found for the given request"),
	HINT_ADDED_SUCCESSFULLY(1007, "Hint added successfully!!"),
	NOTIFICATION_SENT_SUCCESSFULLY(1008, "Mail notification sent successfully!!"),
	CPE_DICTIONARY_UPDATE_SUCCESS(1009, "CPE dictionary updated successfully!!"),
	
	COMPUTER_DELETED(9001, "Cannot update computer as it is deleted. Please restore it first."),
	COMPUTER_INACTIVE(9002, "Cannot update computer as it is inactive. Please activate it first."),
	APPLICATION_ERROR(9003, "Error while processing application data. Please check the input data"),
	DEFAULT_ERROR(9004, "Cannot process request. Please try again later."),
	METHOD_ARGUMENT_NOT_VALID(9005, "Invalid input data. Please check the data and try again."),
	SEARCH_TYPE_MISSING(9006, "Search type is missing or empty. Please provide a valid search type."),
	SEARCH_CRITERIA_MISSING(9007, "Search criteria is missing or empty. PLease provide a valid search type."),
	MISSING_PARAMETER_FOR_LIKELY_CPE_NAME_SEARCH(9008, "Vendor and Product name cannot be empty!!"),
	CPE_NAME_NOT_VALID(9009, "CPE name is not valid!!"),
	ERROR_ADDING_PRODUCT_HINT(9010, "Error while adding product hint!!"),
	ERROR_ADDING_VENDOR_HINT(9011, "Error while adding vendor hint!!"),
	ERROR_ADDING_HINT(9012, "Error while adding hint!!"),
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
