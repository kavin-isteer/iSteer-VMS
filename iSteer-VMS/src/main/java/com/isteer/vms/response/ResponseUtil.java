package com.isteer.vms.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {

	public static ResponseEntity<Object> message(ResponseCode code) {
		BaseResponse response = new BaseResponse(code.getCode(), code.getMessage());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public static ResponseEntity<Object> message(ResponseCode code, HttpStatus status) {
		BaseResponse response = new BaseResponse(code.getCode(), code.getMessage());
		return new ResponseEntity<>(response, status);
	}

	public static <T> ResponseEntity<Object> data(T data) {
		return new ResponseEntity<>(data, HttpStatus.OK);
	}

	public static ResponseEntity<Object> message(int code, String errorMessage, HttpStatus badRequest) {
		BaseResponse response = new BaseResponse(code, errorMessage);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}
}
