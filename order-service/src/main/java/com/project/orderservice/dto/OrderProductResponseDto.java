package com.project.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderProductResponseDto {
    private Long orderProductId;
    private int unitPrice;
    private int quantity;
    private Long orderId;
    private Long productId;
}
