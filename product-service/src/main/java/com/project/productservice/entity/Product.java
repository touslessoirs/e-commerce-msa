package com.project.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Data
@Entity
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
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

    @Builder.Default
    @Column(name = "purchase_start_time")
    private LocalDateTime purchaseStartTime = LocalDateTime.now();

//    @Version
//    private int version; // 낙관적 락

}
