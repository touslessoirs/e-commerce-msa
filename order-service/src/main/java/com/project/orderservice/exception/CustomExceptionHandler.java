package com.project.orderservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponseEntity> handleCustomException(CustomException e) {
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseEntity> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        if (e.getMessage().contains("foreign key constraint fails") && e.getMessage().contains("fk_order_user")) {
            return ErrorResponseEntity.toResponseEntity(ErrorCode.USER_NOT_FOUND);
        }

        ErrorCode errorCode = ErrorCode.DATA_INTEGRITY_VIOLATION;
        return ErrorResponseEntity.toResponseEntity(errorCode);
    }
}
