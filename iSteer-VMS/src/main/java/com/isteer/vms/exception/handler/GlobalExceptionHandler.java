package com.isteer.vms.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.isteer.vms.dto.ErrorMessageDto;
import com.isteer.vms.enums.Message;
import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.response.ResponseCode;
import com.isteer.vms.response.ResponseUtil;

import lombok.extern.log4j.Log4j2;

@ControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorMessageDto> handleBusinessException(BusinessException ex) {
		Message error = ex.getError();

		return new ResponseEntity<>(new ErrorMessageDto(error.getStatusCode(), error.getMessageKey()),
				HttpStatus.BAD_REQUEST);

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
}
