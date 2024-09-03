package com.project.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product extends Timestamped implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long productId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(nullable = false)
    private int stock;

    @Column(length = 50)
    private String category;

    @Column(name = "purchase_start_time")
    private LocalDateTime purchaseStartTime = LocalDateTime.now();
}
