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
}




