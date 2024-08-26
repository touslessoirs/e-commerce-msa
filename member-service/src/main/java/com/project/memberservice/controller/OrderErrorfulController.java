package com.project.memberservice.controller;

import com.project.memberservice.service.OrderErrorfulService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class OrderErrorfulController {

    private final OrderErrorfulService orderErrorfulService;

    public OrderErrorfulController(OrderErrorfulService orderErrorfulService) {
        this.orderErrorfulService = orderErrorfulService;
    }

    @GetMapping("/errorful/case1")
    public ResponseEntity<String> callErrorfulCase1() {
        return orderErrorfulService.callErrorfulCase1();
    }

    @GetMapping("/errorful/case2")
    public ResponseEntity<String> callErrorfulCase2() {
        return orderErrorfulService.callErrorfulCase2();
    }

    @GetMapping("/errorful/case3")
    public ResponseEntity<String> callErrorfulCase3() {
        return orderErrorfulService.callErrorfulCase3();
    }
}