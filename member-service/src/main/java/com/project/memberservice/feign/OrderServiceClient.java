package com.project.memberservice.feign;

import com.project.memberservice.dto.OrderResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    /* 사용자별 주문 내역 조회 */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByMemberId(@RequestHeader("X-Member-Id") String id);

    @GetMapping("/errorful/case1")
    public ResponseEntity<String> getCase1Response();

    @GetMapping("/errorful/case2")
    public ResponseEntity<String> getCase2Response();

    @GetMapping("/errorful/case3")
    public ResponseEntity<String> getCase3Response();

}
