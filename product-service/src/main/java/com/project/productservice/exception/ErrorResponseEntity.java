package com.project.productservice.exception;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}