package com.project.orderservice.controller;

import com.project.orderservice.service.ScheduledTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * scheduler를 수동 실행하기 위한 Controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/scheduler")
public class ScheduledTaskController {

    private final ScheduledTaskManager scheduledTaskManager;

    /* 주문 상태 업데이트 */
    @PostMapping("/orderStatus")
    public ResponseEntity updateOrderStatus() {
        log.info("주문 상태 업데이트 작업 수동 실행");
        scheduledTaskManager.updateOrderStatus();
        return ResponseEntity.ok("주문 상태 업데이트 완료");
    }

}
