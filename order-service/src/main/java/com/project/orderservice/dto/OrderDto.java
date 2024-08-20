package com.project.orderservice.dto;

import lombok.Data;

@Data
public class OrderDto {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private Enum status;

    private Long memberId;
    private Long productId;
}
