package com.project.memberservice.controller;

import com.project.memberservice.service.OrderErrorfulService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * feign client 통신으로 order service에 요청 전송
 */
@RestController
@RequestMapping("/test")
public class OrderErrorfulController {

    private final OrderErrorfulService orderErrorfulService;

    public OrderErrorfulController(OrderErrorfulService orderErrorfulService) {
        this.orderErrorfulService = orderErrorfulService;
    }

    /* 특정 확률로 HTTP 500 상태 코드를 반환 */
    @GetMapping("/errorful/case1")
    public ResponseEntity<String> callErrorfulCase1() {
        return orderErrorfulService.callErrorfulCase1();
    }

    /* 요청이 들어오면, 요청을 10초 동안 차단(지연)한 뒤 HTTP 503 상태 코드(서비스 불가)를 반환 */
    @GetMapping("/errorful/case2")
    public ResponseEntity<String> callErrorfulCase2() {
        return orderErrorfulService.callErrorfulCase2();
    }

    /* 요청이 들어오면 10초 동안 HTTP 500 상태 코드(Internal Server Error)를 반환 */
    @GetMapping("/errorful/case3")
    public ResponseEntity<String> callErrorfulCase3() {
        return orderErrorfulService.callErrorfulCase3();
    }
}