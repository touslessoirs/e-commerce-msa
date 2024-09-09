package com.project.productservice.controller;

import com.project.productservice.service.ScheduledTaskManager;
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

    /* Redis-DB 재고 및 구매 가능 시간 동기화 */
    @PostMapping("/synchronize-stock")
    public ResponseEntity synchronizeDbAndRedis() {
        log.info("Redis-DB 동기화 작업 수동 실행");
        scheduledTaskManager.synchronizeDbAndRedis();
        return ResponseEntity.ok("Redis-DB 동기화 작업이 완료되었습니다.");
    }

}
