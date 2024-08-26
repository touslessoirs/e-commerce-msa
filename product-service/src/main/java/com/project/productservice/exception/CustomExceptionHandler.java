package com.project.productservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
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

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponseEntity> handleGeneralException(Exception e) {
        return ErrorResponseEntity.toResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "GENERAL_EXCEPTION",
                "SERVER-001",
                "서버 오류가 발생했습니다."
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponseEntity> handleDataAccessException(DataAccessException ex) {
        return ErrorResponseEntity.toResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "DATA_ACCESS_EXCEPTION",
                "SERVER-002",
                "데이터베이스 오류가 발생했습니다."
        );
    }
}