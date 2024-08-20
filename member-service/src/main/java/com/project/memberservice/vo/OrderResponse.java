package com.project.memberservice.vo;

import lombok.Data;

import java.util.Date;

@Data
public class OrderResponse {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private Enum status;    //주문 상태
    private Date createdAt; //주문 생성일

    private Long productId;
    private int unitPrice;
}
