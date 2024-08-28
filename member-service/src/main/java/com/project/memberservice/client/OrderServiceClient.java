package com.project.memberservice.client;

import com.project.memberservice.dto.OrderResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/{memberId}/orders")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByMemberId(@PathVariable("memberId") Long memberId);

    // ErrorfulController's endpoints
    @GetMapping("/errorful/case1")
    public ResponseEntity<String> getCase1Response();

    @GetMapping("/errorful/case2")
    public ResponseEntity<String> getCase2Response();

    @GetMapping("/errorful/case3")
    public ResponseEntity<String> getCase3Response();

}
