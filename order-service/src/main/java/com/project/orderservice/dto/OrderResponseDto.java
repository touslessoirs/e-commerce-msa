package com.project.orderservice.dto;

import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.OrderStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderResponseDto {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private OrderStatusEnum status;
    private LocalDateTime createdAt;

    private Long memberId;

    public OrderResponseDto(Order order) {
        this.orderId = order.getOrderId();
        this.totalPrice = order.getTotalPrice();
        this.totalQuantity = order.getTotalQuantity();
        this.status = order.getStatus();
        this.createdAt = order.getCreatedAt();
        this.memberId = order.getMemberId();
    }
}




