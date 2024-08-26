package com.project.memberservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "회원을 찾을 수 없습니다."),
    DUPLICATE_DATA(HttpStatus.CONFLICT, "USER-002", "중복된 데이터가 존재합니다."),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-003", "이미 사용 중인 전화번호입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-004", "이미 사용 중인 이메일입니다."),

    INVALID_ADMIN_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-001", "유효하지 않은 관리자 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}