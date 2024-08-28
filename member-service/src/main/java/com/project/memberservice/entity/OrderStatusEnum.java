package com.project.memberservice.entity;

public enum OrderStatusEnum {
    PAYMENT_PENDING("결제 대기중"),
    PAYMENT_COMPLETED("결제 완료"),
    PAYMENT_FAILED("결제 실패"),

    SHIPPING("배송중"),
    DELIVERED("배송 완료"),

    CANCELLED("주문 취소"),
    RETURN_REQUESTED("반품 신청"),
    RETURN_COMPLETED("반품 완료"),

    ORDER_CONFIRMED("주문 확정");

    private final String description;

    OrderStatusEnum(String description) {
        this.description = description;
    }
}
