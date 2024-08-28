package com.project.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "order_product")
public class OrderProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long orderProductId;

    @Column(name = "unit_price")
    private int unitPrice;

    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "product_id")
    private Long productId;

    public OrderProduct(int unitPrice, int quantity, Order order, Long productId) {
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.order = order;
        this.productId = productId ;
    }
}
