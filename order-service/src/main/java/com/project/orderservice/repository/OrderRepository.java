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
     * 특정 상태의 주문 중에서 주어진 날짜보다 이전에 수정된 주문 목록을 조회한다.
     *
     * @param orderStatusEnum 조회할 주문의 status
     * @param localDateTime 기준 시점(이 날짜 이전에 수정된 주문)
     * @return 해당 조건에 맞는 주문 목록
     */
    List<Order> findAllByStatusAndModifiedAtBefore(OrderStatusEnum orderStatusEnum, LocalDateTime localDateTime);

    /**
     * 특정 회원의 특정 주문 조회
     * 
     * @param orderId 조회하려는 주문의 ID
     * @param memberId 조회하려는 회원의 ID
     * @return 해당 조건에 맞는 주문
     */
    Optional<Order> findByOrderIdAndMemberId(Long orderId, Long memberId);
}
