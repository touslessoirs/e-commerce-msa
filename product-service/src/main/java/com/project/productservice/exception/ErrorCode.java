package com.project.productservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "상품을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT-002", "상품의 재고가 부족합니다."),

    STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "ORDER-003", "재고가 부족하여 결제할 수 없습니다."),
    PURCHASE_TIME_INVALID(HttpStatus.FORBIDDEN, "ORDER-004", "아직 구매 가능한 시간이 아닙니다."),

    GENERAL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-001", "서버 오류가 발생했습니다."),
    DATA_ACCESS_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-002", "데이터베이스 오류가 발생했습니다."),
    CONCURRENCY_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "Error", "재시도 횟수를 초과했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}