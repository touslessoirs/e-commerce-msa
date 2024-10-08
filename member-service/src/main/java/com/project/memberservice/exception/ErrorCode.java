package com.project.memberservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "회원을 찾을 수 없습니다."),
    USER_EXCEPTION(HttpStatus.BAD_REQUEST, "USER-002", "잘못된 요청입니다."),
    INVALID_REQUEST_CONTENT(HttpStatus.BAD_REQUEST, "USER-003", "유효성 검사 오류가 발생했습니다."),
    DUPLICATE_DATA(HttpStatus.CONFLICT, "USER-004", "중복된 데이터가 존재합니다."),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-005", "이미 사용 중인 전화번호입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-006", "이미 사용 중인 이메일입니다."),
    ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "USER-007", "이미 인증된 사용자입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "USER-008", "잘못된 이메일 주소 형식입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER-009", "유효하지 않은 비밀번호입니다."),

    INVALID_ADMIN_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-001", "유효하지 않은 Admin Token 입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-002", "유효하지 않은 Refresh Token 입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "유효하지 않은 Access Token 입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "유효하지 않은 Token 입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH-005", "인증에 실패하였습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.UNAUTHORIZED, "AUTH-006", "유효하지 않은 인증번호입니다."),
    FORBIDDEN_ADMIN_ACCESS(HttpStatus.FORBIDDEN, "AUTH-007", "관리자 권한이 없습니다."),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "상품을 찾을 수 없습니다."),

    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "CART-001", "장바구니 정보를 찾을 수 없습니다."),
    CART_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "CART-002", "장바구니에서 해당 상품의 정보를 찾을 수 없습니다."),

    GENERAL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-001", "서버 오류가 발생했습니다."),
    DATA_ACCESS_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-002", "데이터베이스 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}