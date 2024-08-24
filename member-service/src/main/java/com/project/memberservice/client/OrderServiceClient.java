package com.project.memberservice.client;

import com.project.memberservice.dto.OrderResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/{memberId}/orders")
    public List<OrderResponseDto> getOrdersByMemberId(@PathVariable("memberId") Long memberId);
}
