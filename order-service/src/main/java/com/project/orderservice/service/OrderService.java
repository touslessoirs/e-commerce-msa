package com.project.orderservice.service;

import com.project.orderservice.dto.OrderDto;
import com.project.orderservice.entity.Order;
import com.project.orderservice.exception.OrderNotFoundException;
import com.project.orderservice.repository.OrderRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 상품 주문
     *
     * @param orderDto
     * @return
     */
    public OrderDto createOrder(OrderDto orderDto) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Order order = mapper.map(orderDto, Order.class);

        orderRepository.save(order);

        OrderDto returnValue = mapper.map(order, OrderDto.class);
        return returnValue;
    }

    /**
     * 사용자별 전체 주문 내역 조회
     * @param memberId
     * @return
     */
    public Iterable<Order> getOrdersByMemberId(Long memberId){
        return orderRepository.findByMemberId(memberId);
    }


    /**
     * 주문 상세 조회
     *
     * @param orderId
     * @return
     */
    public OrderDto getOrderByOrderId(Long orderId) throws OrderNotFoundException {
        Order order = orderRepository.findById(orderId).orElse(null);
        if(order == null) {
            throw new OrderNotFoundException();
        }
        OrderDto orderDto = new ModelMapper().map(order, OrderDto.class);

        return orderDto;
    }

}
