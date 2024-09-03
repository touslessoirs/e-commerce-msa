package com.project.orderservice.repository;

import com.project.orderservice.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {

    /**
     * 특정 주문에 해당하는 OrderProduct 목록 조회
     *
     * @param orderId 특정 주문의 id
     * @return 해당 주문에 해당하는 OrderProduct 목록
     */
    List<OrderProduct> findAllByOrderOrderId(Long orderId);
}
