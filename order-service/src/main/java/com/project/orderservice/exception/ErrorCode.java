package com.project.orderservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER-001", "주문을 찾을 수 없습니다."),
    ORDER_FAILED(HttpStatus.BAD_REQUEST, "ORDER-002", "주문에 실패했습니다."),

    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT-001", "결제에 실패했습니다."),
    STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "PAYMENT-002", "재고가 부족하여 결제할 수 없습니다."),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "상품을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}