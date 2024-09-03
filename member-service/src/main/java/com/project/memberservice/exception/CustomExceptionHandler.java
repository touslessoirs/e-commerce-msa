package com.project.memberservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponseEntity> handleCustomException(CustomException e){
        return ErrorResponseEntity.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponseEntity> handleGeneralException(Exception e) {
        ErrorCode errorCode = ErrorCode.GENERAL_EXCEPTION;
        return ErrorResponseEntity.toResponseEntity(errorCode);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseEntity> handleDataAccessException(DataAccessException e) {
        ErrorCode errorCode = ErrorCode.DATA_ACCESS_EXCEPTION;
        return ErrorResponseEntity.toResponseEntity(errorCode);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseEntity> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorCode errorCode = ErrorCode.USER_EXCEPTION;
        return ErrorResponseEntity.toResponseEntity(errorCode);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {

        FieldError fieldError = ex.getBindingResult().getFieldErrors().get(0);
        String fieldName = fieldError.getField();
        Object rejectedValue = fieldError.getRejectedValue();

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_CONTENT;

        ErrorResponseEntity errorResponse = ErrorResponseEntity.builder()
                .status(errorCode.getHttpStatus().value())
                .name(errorCode.name())
                .code(errorCode.getCode())
                .message(fieldName + " 필드의 입력값 [" + rejectedValue + "]이 유효하지 않습니다.")
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseEntity> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        if (e.getMessage().contains("phone_UNIQUE")) {
            return ErrorResponseEntity.toResponseEntity(ErrorCode.PHONE_ALREADY_EXISTS);
        } else if (e.getMessage().contains("email_UNIQUE")) {
            return ErrorResponseEntity.toResponseEntity(ErrorCode.EMAIL_ALREADY_EXISTS);
        } else {
            return ErrorResponseEntity.toResponseEntity(ErrorCode.DUPLICATE_DATA);
        }
    }
}
