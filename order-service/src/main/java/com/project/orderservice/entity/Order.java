package com.project.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order extends Timestamped implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long orderId;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private OrderStatusEnum status = OrderStatusEnum.PAYMENT_PENDING;

    @Column(name = "user_id", nullable = false)
    private Long memberId;

}
