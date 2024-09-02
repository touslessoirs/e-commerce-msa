package com.project.orderservice.event;

import com.project.orderservice.dto.OrderProductRequestDto;
import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.PaymentStatusEnum;

public record PaymentResponseEvent(
        Order order,
        OrderProductRequestDto orderProductRequestDto,
        PaymentStatusEnum status
) {
}
