package com.project.orderservice.entity;

public enum PaymentStatusEnum {
    PAYMENT_PENDING("결제 대기 중"),
    PAYMENT_COMPLETED("결제 완료"),
    PAYMENT_FAILED("결제 실패");

    private final String description;

    PaymentStatusEnum(String description) {
        this.description = description;
    }
}
