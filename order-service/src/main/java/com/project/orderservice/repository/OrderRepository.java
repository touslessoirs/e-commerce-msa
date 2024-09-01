package com.project.orderservice.repository;

import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.OrderStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Iterable<Order> findByMemberId(Long memberId);

//    @Modifying
//    @Query("UPDATE Order o SET o.status = 'ORDER_FAILED' WHERE o.id = :orderId")
//    void cancelOrder(@Param("orderId") Long orderId);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :orderId")
    void updateOrderStatus(@Param("orderId") Long orderId, @Param("status") OrderStatusEnum status);
}
