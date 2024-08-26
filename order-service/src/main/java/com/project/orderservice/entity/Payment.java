package com.project.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long paymentId;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private PaymentStatusEnum status = PaymentStatusEnum.PAYMENT_PENDING;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
