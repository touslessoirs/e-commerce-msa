package com.project.memberservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "cart_product")
public class CartProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long cartProductId;

//    @Column(name = "unit_price")
//    private int unitPrice;

    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Column(name = "product_id")
    private Long productId;

    public CartProduct(int quantity, Cart cart, Long productId) {
//        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.cart = cart;
        this.productId = productId;
    }
}
