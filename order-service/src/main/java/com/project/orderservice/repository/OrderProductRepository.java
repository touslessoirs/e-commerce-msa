package com.project.orderservice.repository;

import com.project.orderservice.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    List<OrderProduct> findByOrderOrderId(Long orderId);
}
