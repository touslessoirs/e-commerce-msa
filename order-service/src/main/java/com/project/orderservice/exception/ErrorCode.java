package com.project.orderservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),
    ORDER_FAILED(HttpStatus.BAD_REQUEST, "ORDER-002", "주문에 실패했습니다."),
    ORDER_REQUEST_DENIED(HttpStatus.BAD_REQUEST, "ORDER-003", "주문 요청이 거부되었습니다."),
    CANCELLATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ORDER-004", "해당 주문은 취소할 수 없습니다."),
    RETURN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ORDER-005", "해당 주문은 반품 신청이 불가능합니다."),
    ORDER_IS_NOT_RETURN_REQUESTED(HttpStatus.BAD_REQUEST, "ORDER-006", "반품 신청이 접수되지 않은 상태입니다."),

    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT-001", "결제에 실패했습니다."),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "상품을 찾을 수 없습니다."),
    PURCHASE_TIME_INVALID(HttpStatus.FORBIDDEN, "PRODUCT-002", "현재 구매가 불가능한 상품이 포함되어 있습니다."),
    STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "PRODUCT-003", "재고가 부족하여 결제할 수 없는 상품이 포함되어 있습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "회원을 찾을 수 없습니다."),

    GENERAL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-001", "서버 오류가 발생했습니다."),
    DATA_ACCESS_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-002", "데이터베이스 오류가 발생했습니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-003", "데이터 무결성 위반 오류가 발생했습니다."),

    ORDER_LOCK_FAILED(HttpStatus.CONFLICT, "ORDER_LOCK-001", "LOCK을 획득하지 못했습니다. 다른 프로세스가 이 주문을 처리 중일 수 있습니다."),
    ORDER_LOCK_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_LOCK-002", "LOCK 처리 중에 인터럽트가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}