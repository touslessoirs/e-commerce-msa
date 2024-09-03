package com.project.orderservice.service;

import com.project.orderservice.dto.PaymentResponseDto;
import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.Payment;
import com.project.orderservice.entity.PaymentStatusEnum;
import com.project.orderservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제하기
     *
     * @param order
     * @return
     */
    public PaymentResponseDto processPayment(Order order) {
        // 1. 주문 정보를 바탕으로 결제 정보 생성
        Payment payment = createPayment(order);

        // 2. 결제 처리 및 결제 정보 저장
        Payment savedPayment = savePayment(payment);

        // 3. 결제 성공 여부 반환
        return new PaymentResponseDto(savedPayment);
    }

    /**
     * 결제 정보 생성
     *
     * @param order
     * @return Payment
     */
    public Payment createPayment(Order order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(PaymentStatusEnum.PAYMENT_PENDING);

        return payment;
    }

    /**
     * 결제 처리 및 결제 정보 저장
     * PAYMENT_PENDING -> PAYMENT_COMPLETED
     * 고객 이탈율 시나리오 : 이탈율 20%
     *
     * @param payment
     * @return
     */
    @Transactional
    public Payment savePayment(Payment payment) {
        // PAYMENT_PENDING -> PAYMENT_COMPLETED 과정에서 이탈
        // ex) 잔액 부족으로 인한 결제 실패
        if (Math.random() < 0.20) { //20%
            payment.setStatus(PaymentStatusEnum.PAYMENT_FAILED);
        } else {
            payment.setStatus(PaymentStatusEnum.PAYMENT_COMPLETED);
        }

        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment == null) {
            throw new RuntimeException("결제 정보 저장 중 문제가 발생했습니다.");
        }

        log.info("결제 정보 저장 완료");
        log.info(String.valueOf(savedPayment.getStatus()));
        return savedPayment;
    }
}
