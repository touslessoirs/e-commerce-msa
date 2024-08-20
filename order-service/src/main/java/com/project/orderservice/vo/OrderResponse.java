package com.project.orderservice.vo;

import lombok.Data;

@Data
public class OrderResponse {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private Enum status;

    private Long memberId;
}
