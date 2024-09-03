package com.project.memberservice.service;

import com.project.memberservice.client.OrderServiceClient;
import com.project.memberservice.exception.FeignErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderErrorfulService {

    private final OrderServiceClient orderServiceClient;
    private final FeignErrorDecoder feignErrorDecoder;
    private final CircuitBreakerFactory circuitBreakerFactory;

    public ResponseEntity<String> callErrorfulCase1() {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
        return circuitBreaker.run(() -> orderServiceClient.getCase1Response(),
                throwable -> ResponseEntity.status(500).body("Fallback response for case1"));
    }

    public ResponseEntity<String> callErrorfulCase2() {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
        return circuitBreaker.run(() -> orderServiceClient.getCase2Response(),
                throwable -> ResponseEntity.status(503).body("Fallback response for case2"));
    }

    public ResponseEntity<String> callErrorfulCase3() {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
        return circuitBreaker.run(() -> orderServiceClient.getCase3Response(),
                throwable -> ResponseEntity.status(500).body("Fallback response for case3"));
    }
}
