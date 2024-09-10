package com.project.productservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "상품을 찾을 수 없습니다."),
    PURCHASE_TIME_INVALID(HttpStatus.FORBIDDEN, "PRODUCT-002", "현재 구매가 불가능한 상품이 포함되어 있습니다."),
    STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "PRODUCT-003", "재고가 부족합니다."),

    GENERAL_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-001", "서버 오류가 발생했습니다."),
    DATA_ACCESS_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER-002", "데이터베이스 오류가 발생했습니다."),

    FORBIDDEN_ADMIN_ACCESS(HttpStatus.FORBIDDEN, "AUTH-007", "관리자 권한이 없습니다."),

    COULD_NOT_ACQUIRE_LOCK(HttpStatus.CONFLICT, "P_LOCK_001", "락을 획득할 수 없습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}