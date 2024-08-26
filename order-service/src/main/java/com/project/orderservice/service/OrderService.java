package com.project.orderservice.service;

import com.project.orderservice.client.ProductServiceClient;
import com.project.orderservice.dto.OrderProductRequestDto;
import com.project.orderservice.dto.OrderRequestDto;
import com.project.orderservice.dto.OrderResponseDto;
import com.project.orderservice.dto.ProductResponseDto;
import com.project.orderservice.entity.Order;
import com.project.orderservice.entity.OrderProduct;
import com.project.orderservice.entity.Shipping;
import com.project.orderservice.exception.FeignErrorDecoder;
import com.project.orderservice.exception.OrderNotFoundException;
import com.project.orderservice.repository.OrderProductRepository;
import com.project.orderservice.repository.OrderRepository;
import com.project.orderservice.repository.ShippingRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final FeignErrorDecoder feignErrorDecoder;
    private final OrderProductRepository orderProductRepository;
    private final ShippingRepository shippingRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductServiceClient productServiceClient,
                        FeignErrorDecoder feignErrorDecoder, OrderProductRepository orderProductRepository, ShippingRepository shippingRepository) {
        this.orderRepository = orderRepository;
        this.productServiceClient = productServiceClient;
        this.feignErrorDecoder = feignErrorDecoder;
        this.orderProductRepository = orderProductRepository;
        this.shippingRepository = shippingRepository;
    }

    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderRequestDto orderRequestDto) {
        List<OrderProduct> orderProductList = new ArrayList<>();
        int totalQuantity = 0;
        int totalPrice = 0;
        Order order = new Order();

        //상품 상세 페이지에서 주문한 경우
        log.info("상품 상세 페이지에서 주문 요청");
        for (OrderProductRequestDto orderProductDto : orderRequestDto.getOrderProducts()) {
            Long productId = orderProductDto.getId();

            //Product 통신 -> 상품 상세 조회
            ProductResponseDto product = productServiceClient.getProductDetail(productId);

            int quantity = orderProductDto.getQuantity();
            int price = orderProductDto.getPrice() * quantity;  //해당 상품의 총 가격

            //Product 통신 -> 재고 반영
            productServiceClient.updateStock(productId, quantity);

            // 주문 상품 정보 생성
            OrderProduct orderProduct = new OrderProduct(price, quantity, order, productId);
            orderProductList.add(orderProduct);

            totalQuantity += quantity;
            totalPrice += price;
        }

        // 주문 정보 설정
        order.setMemberId(memberId);
        order.setTotalQuantity(totalQuantity);
        order.setTotalPrice(totalPrice);

        // 주문 저장
        orderRepository.save(order);
        log.info("주문 정보 저장 완료");

        orderProductRepository.saveAll(orderProductList);
        log.info("주문 상품 정보 저장 완료");

        // 배송 정보 저장
        Shipping shipping = new Shipping();
        shipping.setAddress(orderRequestDto.getShipping().getAddress());
        shipping.setAddressDetail(orderRequestDto.getShipping().getAddressDetail());
        shipping.setPhone(orderRequestDto.getShipping().getPhone());
        shipping.setOrder(order);
        shippingRepository.save(shipping);
        log.info("배송 정보 저장 완료");

        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setOrderId(order.getOrderId());
        orderResponseDto.setTotalPrice(totalPrice);
        orderResponseDto.setTotalQuantity(totalQuantity);
        orderResponseDto.setStatus(order.getStatus());
        orderResponseDto.setMemberId(memberId);

        return orderResponseDto;
    }

        /**
         * 사용자별 전체 주문 내역 조회
         * @param memberId
         * @return
         */
        public List<OrderResponseDto> getOrdersByMemberId(Long memberId) {
            Iterable<Order> orders = orderRepository.findByMemberId(memberId);

            //Iterable to List
            List<Order> orderList = StreamSupport.stream(orders.spliterator(), false)
                    .collect(Collectors.toList());

            //Order to OrderResponseDto
            ModelMapper modelMapper = new ModelMapper();
            return orderList.stream()
                    .map(order -> modelMapper.map(order, OrderResponseDto.class))
                    .collect(Collectors.toList());
        }

        /**
         * 주문 상세 조회
         *
         * @param orderId
         * @return
         */
        public OrderResponseDto getOrderDetail (Long orderId) throws OrderNotFoundException {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new OrderNotFoundException();
            }
            OrderResponseDto orderResponseDto = new ModelMapper().map(order, OrderResponseDto.class);

            return orderResponseDto;
        }

    }
