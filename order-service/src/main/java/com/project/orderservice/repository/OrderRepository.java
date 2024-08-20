package com.project.orderservice.repository;

import com.project.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Iterable<Order> findByMemberId(Long memberId);
}
