package com.project.orderservice.dto;

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

    public OrderResponseDto(Long orderId, int totalPrice, int totalQuantity, OrderStatusEnum status, LocalDateTime createdAt, Long memberId) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.createdAt = createdAt;
        this.memberId = memberId;
    }
}




