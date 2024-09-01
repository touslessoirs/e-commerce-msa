package com.project.productservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponseEntity> handleCustomException(CustomException e){
        return ErrorResponseEntity.toResponseEntity(e.getErrorCode());
    }

//    @ExceptionHandler(Exception.class)
//    protected ResponseEntity<ErrorResponseEntity> handleGeneralException(Exception e) {
//        ErrorCode errorCode = ErrorCode.GENERAL_EXCEPTION;
//        return ErrorResponseEntity.toResponseEntity(errorCode);
//    }

//    @ExceptionHandler(DataAccessException.class)
//    public ResponseEntity<ErrorResponseEntity> handleDataAccessException(DataAccessException e) {
//        ErrorCode errorCode = ErrorCode.DATA_ACCESS_EXCEPTION;
//        return ErrorResponseEntity.toResponseEntity(errorCode);
//    }
}