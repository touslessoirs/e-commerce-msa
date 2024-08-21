package com.project.orderservice.controller;

import com.project.orderservice.dto.OrderDto;
import com.project.orderservice.entity.Order;
import com.project.orderservice.service.OrderService;
import com.project.orderservice.vo.Greeting;
import com.project.orderservice.vo.OrderRequest;
import com.project.orderservice.vo.OrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order-service")
public class OrderController {

    private final Environment env;
    private final OrderService orderService;

    public OrderController(Environment env, OrderService orderService) {
        this.env = env;
        this.orderService = orderService;
    }

    @Autowired
    private Greeting greeting;

    @GetMapping("/health-check")
    public String status() {
        return String.format("ORDER SERVICE Running on PORT %s", env.getProperty("server.port"));
    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting.getMessage();
    }

    /* 상품 주문 */
    @PostMapping("/{memberId}/orders")
    public ResponseEntity<OrderResponse> createOrder(@PathVariable("memberId") Long memberId, @RequestBody OrderRequest orderRequest) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        OrderDto orderDto = mapper.map(orderRequest, OrderDto.class);
        orderDto.setMemberId(memberId);
        OrderDto createdOrder = orderService.createOrder(orderDto);
        OrderResponse orderResponse = mapper.map(createdOrder, OrderResponse.class);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);

    }

    /* 사용자별 전체 주문 내역 조회 */
    @GetMapping("/{memberId}/orders")
    public ResponseEntity<List<OrderResponse>> getOrdersByMemberId(@PathVariable("memberId") Long memberId) {
        Iterable<Order> orderList = orderService.getOrdersByMemberId(memberId);

        List<OrderResponse> result = new ArrayList<>();
        orderList.forEach(v -> {
            result.add(new ModelMapper().map(v, OrderResponse.class));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 주문 상세 조회 */
    //getOrderByOrderId
//    @GetMapping("/{orderId}")
}
