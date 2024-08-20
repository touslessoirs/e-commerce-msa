package com.project.orderservice.vo;

import lombok.Data;

@Data
public class OrderRequest {
    private Long productId;
    private int totalPrice;
    private int totalQuantity;
}
