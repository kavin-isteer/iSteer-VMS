package com.isteer.vms.dto;

public class ErrorMessageDto {
	
	private int errorCode;
	private String errorMessage;

	public ErrorMessageDto() {
		
	}
	
	public ErrorMessageDto(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public ErrorMessageDto(int errorCode, String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
	
	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
