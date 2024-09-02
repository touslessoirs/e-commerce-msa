package com.project.orderservice.event;

import com.project.orderservice.entity.Order;

public record ShippingRequestEvent(
        String address,
        String addressDetail,
        String phone,
        Order order
) {
}
