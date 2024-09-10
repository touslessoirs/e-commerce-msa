package com.project.orderservice.controller;

import com.project.orderservice.dto.OrderRequestDto;
import com.project.orderservice.dto.OrderResponseDto;
import com.project.orderservice.entity.OrderStatusEnum;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class OrderController {

    @Value("${greeting.message}")
    private String greeting;

    @Value("${server.port}")
    private String port;

    private final OrderService orderService;

    @GetMapping("/health-check")
    public String status() {
        return String.format("ORDER SERVICE Running on PORT %s", port);
    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting;
    }

    /* 주문 요청 - 주문 가능 여부 확인 -> 주문 생성 및 결제 요청 */
    @PostMapping("/orders/request")
    public ResponseEntity<OrderResponseDto> requestOrder(@RequestHeader("X-Member-Id") String id,
                                                         @Valid @RequestBody OrderRequestDto orderRequestDto) {
        // 구매 가능 시간 & 재고 확인
        boolean isAvailable = orderService.requestOrder(orderRequestDto);

        try {
            if (isAvailable) {
                log.info("주문 가능");

                OrderResponseDto orderResponseDto = orderService.createOrder(id, orderRequestDto);
                
                // 주문 성패 여부 분기 처리
                if (orderResponseDto.getStatus() == OrderStatusEnum.PAYMENT_COMPLETED) {
                    log.info("결제 성공");
                    return ResponseEntity.status(HttpStatus.CREATED).body(orderResponseDto);
                } else {
                    orderService.rollbackStock(orderRequestDto.getOrderProducts());
                    if (orderResponseDto.getStatus() == OrderStatusEnum.PAYMENT_FAILED) {
                        throw new CustomException(ErrorCode.PAYMENT_FAILED);
                    } else {
                        throw new CustomException(ErrorCode.ORDER_FAILED);
                    }
                }
            } else {
                //주문 불가능 -> 클라이언트 응답 반환
                log.info("주문 불가능");
                throw new CustomException(ErrorCode.ORDER_REQUEST_DENIED);
            }
        } catch (DataAccessException e) {
            orderService.rollbackStock(orderRequestDto.getOrderProducts());
            throw new CustomException(ErrorCode.DATA_ACCESS_EXCEPTION, e);
        }
    }

    /* 사용자별 전체 주문 내역 조회 */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> getOrdersByMemberId(@RequestHeader("X-Member-Id") String id) {
        Iterable<OrderResponseDto> orderList = orderService.getOrdersByMemberId(id);

        List<OrderResponseDto> result = new ArrayList<>();
        orderList.forEach(result::add);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 주문 상세 조회 */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderDetail(@RequestHeader("X-Member-Id") String id,
                                                           @PathVariable("orderId") Long orderId) {
        OrderResponseDto orderResponseDto = orderService.getOrderDetail(id, orderId);
        return ResponseEntity.ok(orderResponseDto);
    }

    /* 주문 취소 */
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity cancelOrder(@RequestHeader("X-Member-Id") String id, @PathVariable Long orderId) {
        orderService.cancelOrder(id, orderId);
        return ResponseEntity.ok("주문이 취소되었습니다.");
    }

    /* 반품 신청 */
    @PostMapping("/return/{orderId}")
    public ResponseEntity requestReturn(@RequestHeader("X-Member-Id") String id, @PathVariable Long orderId) {
        orderService.requestReturn(id, orderId);
        return ResponseEntity.ok("반품 신청이 완료되었습니다.");
    }

    /* 반품 신청 승인 */
    @PostMapping("/approve-return/{orderId}")
    public ResponseEntity<String> approveReturnRequest(@PathVariable Long orderId) {
        orderService.approveReturnRequest(orderId);
        return ResponseEntity.ok("반품 신청이 승인되었습니다.");
    }

}
