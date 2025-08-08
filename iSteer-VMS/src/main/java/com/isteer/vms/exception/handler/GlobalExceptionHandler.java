package com.isteer.vms.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.exception.NvdApiException;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;

import lombok.extern.log4j.Log4j2;

@ControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<Object> handleBusinessException(BusinessException ex) {
		return ResponseUtil.message(9010, ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode()));
	}
	
	@ExceptionHandler(NvdApiException.class)
	public ResponseEntity<Object> handleNvdApiExcption(NvdApiException ex) {
		return ResponseUtil.message(9009, ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
	    // Get first error
	    var fieldError = ex.getBindingResult().getFieldError();

	    // Fallback to default message from enum if none present
	    String errorMessage = (fieldError != null && fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank())
	        ? fieldError.getDefaultMessage()
	        : ResponseCode.METHOD_ARGUMENT_NOT_VALID.getMessage();

	    return ResponseUtil.message(ResponseCode.METHOD_ARGUMENT_NOT_VALID.getCode(), errorMessage, HttpStatus.BAD_REQUEST);
	}
	
	public ResponseEntity<Object> handleAllException(Exception ex) {
		return ResponseUtil.message(9999, ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
