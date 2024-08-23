package com.project.memberservice.client;

import com.project.memberservice.vo.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/order-service/{memberId}/orders")
    public List<OrderResponse> getOrdersByMemberId(@PathVariable("memberId") Long memberId);
}
