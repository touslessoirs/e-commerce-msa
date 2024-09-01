package com.project.orderservice.dto;

import com.project.orderservice.entity.Payment;
import com.project.orderservice.entity.PaymentStatusEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponseDto {
    private Long paymentId;
    private PaymentStatusEnum status;
    private Long orderId;

    public PaymentResponseDto(Payment payment) {
        this.paymentId = payment.getPaymentId();
        this.status = payment.getStatus();
        this.orderId = payment.getOrder().getOrderId();
    }
}
