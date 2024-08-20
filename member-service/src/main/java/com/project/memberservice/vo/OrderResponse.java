package com.project.memberservice.vo;

import lombok.Data;

import java.util.Date;

@Data
public class OrderResponse {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private Enum status;

    private Long productId;
    private int unitPrice;
    private Date createdAt;
}
