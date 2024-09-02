package com.project.orderservice.event;

import com.project.orderservice.dto.OrderProductRequestDto;
import com.project.orderservice.entity.Order;

public record PaymentRequestEvent(
        Order order,
        OrderProductRequestDto orderProductRequestDto
) {
}
