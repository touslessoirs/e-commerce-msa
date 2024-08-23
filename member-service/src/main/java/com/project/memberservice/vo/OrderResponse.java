package com.project.memberservice.vo;

import com.project.memberservice.entity.OrderStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponse {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private OrderStatusEnum status;     //주문 상태
    private LocalDateTime createdAt;    //주문 생성일
}
