package com.project.orderservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.Random;

/**
 * 임의로 Error 발생시키기 위한 Controller
 */
@RestController
@RequestMapping("/errorful")
public class ErrorfulController {

    /* 특정 확률로 HTTP 500 상태 코드를 반환 */
    @GetMapping("/case1")
    public ResponseEntity<String> case1() {
        if (new Random().nextInt(100) < 5) {  //5%
//        if (new Random().nextInt(10) < 5) { //50%
            return ResponseEntity.status(500).body("Internal Server Error");
        }

        return ResponseEntity.ok("Normal response");
    }

    /* 요청이 들어오면, 요청을 10초 동안 차단(지연)한 뒤 HTTP 503 상태 코드(서비스 불가)를 반환 */
    @GetMapping("/case2")
    public ResponseEntity<String> case2() {
        // Simulate blocking requests every first 10 seconds
        LocalTime currentTime = LocalTime.now();
        int currentSecond = currentTime.getSecond();

        if (currentSecond < 10) {
            // Simulate a delay (block) for 10 seconds
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.status(503).body("Service Unavailable");
        }

        return ResponseEntity.ok("Normal response");
    }

    /* 요청이 들어오면 10초 동안 HTTP 500 상태 코드(Internal Server Error)를 반환 */
    @GetMapping("/case3")
    public ResponseEntity<String> case3() {
        // Simulate 500 error every first 10 seconds
        LocalTime currentTime = LocalTime.now();
        int currentSecond = currentTime.getSecond();

        if (currentSecond < 10) {
            return ResponseEntity.status(500).body("Internal Server Error");
        }

        return ResponseEntity.ok("Normal response");
    }
}