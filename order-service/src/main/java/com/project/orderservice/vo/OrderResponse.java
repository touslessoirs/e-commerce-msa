package com.project.orderservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.orderservice.entity.OrderStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private Long orderId;
    private int totalPrice;
    private int totalQuantity;
    private OrderStatusEnum status;     //주문 상태
    private LocalDateTime createdAt;    //주문 생성일
}