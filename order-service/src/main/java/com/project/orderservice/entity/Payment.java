package com.project.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
public class Payment extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long paymentId;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private PaymentStatusEnum status = PaymentStatusEnum.PAYMENT_PENDING;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
