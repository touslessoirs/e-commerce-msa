package com.project.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order extends Timestamped implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long orderId;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private OrderStatusEnum status = OrderStatusEnum.PAYMENT_COMPLETED;

    @Column(name = "user_id", nullable = false)
    private Long memberId;

}
