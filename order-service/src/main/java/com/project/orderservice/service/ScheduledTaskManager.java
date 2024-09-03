package com.project.orderservice.service;

import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.OrderStatusEnum;
import com.project.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@EnableKafka
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskManager {

    private static final String ROLLBACK_TOPIC = "rollback-topic";

    private final OrderRepository orderRepository;

    /**
     * 주문 상태 업데이트
     * 1. 결제 대기 중 상태에서 1일 지남 → 주문 취소
     * 2. 결제 완료 상태에서 1일 지남 → 배송 중 (임의로 가정)
     * 3. 배송 중 상태에서 2일 지남 → 배송 완료 (임의로 가정)
     * 4. 배송 완료 상태에서 3일 지남 → 주문 확정
     */
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정 실행
    public void updateOrderStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 결제 대기 → 주문 취소
        List<Order> ordersToCancelled = orderRepository.findAllByStatusAndModifiedAtBefore(
                OrderStatusEnum.PAYMENT_PENDING, now.minusDays(1));
        for (Order order : ordersToCancelled) {
            order.setStatus(OrderStatusEnum.CANCELLED);
        }

        // 2. 결제 완료 → 배송 중
        List<Order> ordersToShipping = orderRepository.findAllByStatusAndModifiedAtBefore(
                OrderStatusEnum.PAYMENT_COMPLETED, now.minusDays(1));
        for (Order order : ordersToShipping) {
            order.setStatus(OrderStatusEnum.SHIPPING);
        }

        // 3. 배송 중 -> 배송 완료
        List<Order> ordersToDelivered = orderRepository.findAllByStatusAndModifiedAtBefore(
                OrderStatusEnum.SHIPPING, now.minusDays(2));
        for (Order order : ordersToDelivered) {
            order.setStatus(OrderStatusEnum.DELIVERED);
        }

        // 4. 배송 완료 -> 주문 확정
        List<Order> ordersToConfirmed = orderRepository.findAllByStatusAndModifiedAtBefore(
                OrderStatusEnum.DELIVERED, now.minusDays(3));
        for (Order order : ordersToConfirmed) {
            order.setStatus(OrderStatusEnum.ORDER_CONFIRMED);
        }

        orderRepository.saveAll(ordersToCancelled);
        orderRepository.saveAll(ordersToShipping);
        orderRepository.saveAll(ordersToDelivered);
        orderRepository.saveAll(ordersToConfirmed);

        log.info("주문 상태 업데이트 완료");
    }

}
