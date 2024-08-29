package com.project.orderservice.controller;

import com.project.orderservice.dto.OrderRequestDto;
import com.project.orderservice.dto.OrderResponseDto;
import com.project.orderservice.entity.OrderStatusEnum;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.messageQueue.KafkaProducer;
import com.project.orderservice.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/")
public class OrderController {

    @Value("${greeting.message}")
    private String greeting;
    private final Environment env;
    private final OrderService orderService;
    private final KafkaProducer kafkaProducer;

    public OrderController(Environment env, OrderService orderService, KafkaProducer kafkaProducer) {
        this.env = env;
        this.orderService = orderService;
        this.kafkaProducer = kafkaProducer;
    }

    @GetMapping("/health-check")
    public String status() {
        return String.format("ORDER SERVICE Running on PORT %s", env.getProperty("server.port"));
    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting;
    }

    /* 주문하기 */
    @PostMapping("/{memberId}/orders")
    public ResponseEntity<OrderResponseDto> createOrder(@PathVariable("memberId") Long memberId,
                                      @Valid @RequestBody OrderRequestDto orderRequestDto) {

        OrderResponseDto orderResponseDto = orderService.createOrder(memberId, orderRequestDto);

        //결제 성공여부 분기 처리
        if (orderResponseDto.getStatus() == OrderStatusEnum.PAYMENT_COMPLETED) {
            log.info("결제 성공");
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponseDto);
        } else if (orderResponseDto.getStatus() == OrderStatusEnum.PAYMENT_FAILED) {
            //재고 rollback
            log.info("결제 실패");
            orderService.rollbackStock(orderRequestDto);
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
        throw new CustomException(ErrorCode.ORDER_FAILED);
    }

    /* 사용자별 전체 주문 내역 조회 */
    @GetMapping("/{memberId}/orders")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByMemberId(@PathVariable("memberId") Long memberId) {
        Iterable<OrderResponseDto> orderList = orderService.getOrdersByMemberId(memberId);

        List<OrderResponseDto> result = new ArrayList<>();
        orderList.forEach(result::add);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 주문 상세 조회 */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderDetail(@PathVariable("orderId") Long orderId) {
        OrderResponseDto orderResponseDto = orderService.getOrderDetail(orderId);
        return ResponseEntity.ok(orderResponseDto);
    }
}
