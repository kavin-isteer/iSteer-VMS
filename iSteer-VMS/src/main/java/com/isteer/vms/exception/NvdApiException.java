package com.isteer.vms.exception;

public class NvdApiException extends RuntimeException {
	private final int statusCode;

	public NvdApiException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}
}
