package com.isteer.vms.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.isteer.vms.dto.ErrorMessageDto;
import com.isteer.vms.enums.Message;
import com.isteer.vms.exception.BusinessException;

import lombok.extern.log4j.Log4j2;

@ControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorMessageDto> handleBusinessException(BusinessException ex) {
		Message error = ex.getError();
		
		return new ResponseEntity<>(new ErrorMessageDto(error.getStatusCode(), error.getMessageKey()), HttpStatus.BAD_REQUEST);
		
	}
//	@ExceptionHandler(BussinessException.class)
//	public ResponseEntity<ErrorMessageDto> handleBusinessException(BussinessException ex) {
//		Message error = ex.getError();
//		log.error("Business error: {}", ex.getMessage());
//		return new ResponseEntity<>(new ErrorMessageDTO(error.getStatusCode(), StatusMessageUtil.getMessage(error)),
//				HttpStatus.BAD_REQUEST);
//	}
	
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorMessageDto> handleValidationException(MethodArgumentNotValidException ex) {
		FieldError fieldError = ex.getBindingResult().getFieldError();
		String errorMesssage = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
		return new ResponseEntity<>(new ErrorMessageDto(Message.VALIDATION_ERROR.getStatusCode(), errorMesssage), HttpStatus.BAD_REQUEST);
	}
//	@ExceptionHandler(MethodArgumentNotValidException.class)
//	public ResponseEntity<ErrorMessageDto> handleValidationException(MethodArgumentNotValidException ex) {
//		FieldError fieldError = ex.getBindingResult().getFieldError();
//		String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
//		log.error("Validation error: {}", errorMessage);
//		return new ResponseEntity<>(new ErrorMessageDTO(CVSSEnum.VALIDATION_ERROR.getStatusCode(), errorMessage),
//				HttpStatus.BAD_REQUEST);
//	}
}
