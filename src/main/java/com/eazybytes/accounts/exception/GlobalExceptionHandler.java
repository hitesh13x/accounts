package com.eazybytes.accounts.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.eazybytes.accounts.dto.ErrorResponseDto;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	@Nullable
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode httpStatusCode, WebRequest webRequest) {
		Map<String, String> validationErrors = new HashMap<>();
		List<ObjectError> validationErrorList = exception.getAllErrors();
		validationErrorList.forEach(err -> {
			validationErrors.put(((FieldError) err).getField(), err.getDefaultMessage());
		});
		return new ResponseEntity<>(validationErrors, httpStatusCode);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception exception,
			WebRequest webRequest) {
		ErrorResponseDto errorResponseDto = new ErrorResponseDto(
				webRequest.getDescription(false),
				HttpStatus.INTERNAL_SERVER_ERROR,
				exception.getMessage(),
				LocalDateTime.now());
		return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(CustomerAlreadyExistsException.class)
	public ResponseEntity<ErrorResponseDto> handleCustomerAlreadyExistsException(
			CustomerAlreadyExistsException exception,
			WebRequest webRequest) {
		ErrorResponseDto errorResponseDto = new ErrorResponseDto(
				webRequest.getDescription(false),
				HttpStatus.BAD_REQUEST,
				exception.getMessage(),
				LocalDateTime.now());
		return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException exception,
			WebRequest webRequest) {
		ErrorResponseDto errorResponseDto = new ErrorResponseDto(
				webRequest.getDescription(false),
				HttpStatus.NOT_FOUND,
				exception.getMessage(),
				LocalDateTime.now());
		return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);

	}
}
