package com.project.orderservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "orders")
public class Order implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long orderId;

    @Column(name = "total_price", nullable = false)
    private String totalPrice;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private OrderStatusEnum status = OrderStatusEnum.PAYMENT_COMPLETED;

    @Column(name = "user_id", nullable = false)
    private Long memberId;

}
