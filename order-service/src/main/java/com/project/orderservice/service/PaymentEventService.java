package com.project.orderservice.service;

import com.project.orderservice.entity.Payment;
import com.project.orderservice.event.PaymentRequestEvent;
import com.project.orderservice.event.PaymentResponseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@EnableKafka
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private static final String PAYMENT_REQUEST_TOPIC = "payment-request-topic";
    private static final String PAYMENT_RESPONSE_TOPIC = "payment-response-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentService paymentService;

    /**
     * PaymentRequestEvent event 수신
     *
     * @param event
     */
    @KafkaListener(topics = PAYMENT_REQUEST_TOPIC)
    public void listenPaymentRequestEvent(PaymentRequestEvent event) {
        log.info("Received PaymentRequestEvent: productID: {}, orderId: {}", event.orderProductRequestDto().getProductId(), event.order().getOrderId());

        // 1. 주문 정보를 바탕으로 결제 정보 생성
        Payment payment = paymentService.createPayment(event.order());

        // 2. 결제 처리 및 결제 정보 저장
        Payment savedPayment = paymentService.savePayment(payment);

        // 3. 결제 정보 응답 event send
        PaymentResponseEvent resultEvent = new PaymentResponseEvent(
                event.order(),
                event.orderProductRequestDto(),
                savedPayment.getStatus()
        );
        kafkaTemplate.send(PAYMENT_RESPONSE_TOPIC, resultEvent);
    }
}
