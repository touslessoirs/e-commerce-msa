package com.project.orderservice.repository;

import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.OrderStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Iterable<Order> findByMemberId(Long memberId);

    /**
     * @param orderStatusEnum 주문상태
     * @param localDateTime 기준 시점
     * @return 해당 조건에 맞는 주문 목록
     */
    List<Order> findAllByStatusAndModifiedAtBefore(OrderStatusEnum orderStatusEnum, LocalDateTime localDateTime);

    /**
     * 특정 회원의 특정 주문 조회
     * 
     * @param orderId
     * @param memberId
     * @return 해당 조건에 맞는 주문
     */
    Optional<Order> findByOrderIdAndMemberId(Long orderId, Long memberId);
}
