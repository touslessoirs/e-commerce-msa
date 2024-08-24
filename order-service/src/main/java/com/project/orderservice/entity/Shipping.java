package com.project.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shipping")
public class Shipping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long shippingId;

    private String address;

    private String addressDetail;

    private String phone;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
