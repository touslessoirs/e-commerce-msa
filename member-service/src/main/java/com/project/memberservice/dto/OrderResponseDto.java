package com.project.memberservice.dto;

import com.project.memberservice.entity.OrderStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponseDto {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private OrderStatusEnum status;
    private LocalDateTime createdAt;
}
