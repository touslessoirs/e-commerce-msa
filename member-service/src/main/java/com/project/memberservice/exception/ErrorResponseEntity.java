package com.project.memberservice.exception;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Data
@Builder
public class ErrorResponseEntity {
    private int status;
    private String name;
    private String code;
    private String message;

    public static ResponseEntity<ErrorResponseEntity> toResponseEntity(ErrorCode e){
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ErrorResponseEntity.builder()
                        .status(e.getHttpStatus().value())
                        .name(e.name())
                        .code(e.getCode())
                        .message(e.getMessage())
                        .build());
    }

    public static ResponseEntity<ErrorResponseEntity> toResponseEntity(HttpStatus httpStatus, String name, String code, String message) {
        return ResponseEntity
                .status(httpStatus)
                .body(ErrorResponseEntity.builder()
                        .status(httpStatus.value())
                        .name(name)
                        .code(code)
                        .message(message)
                        .build());
    }

    public static ResponseEntity<ErrorResponseEntity> fromMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
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
}
