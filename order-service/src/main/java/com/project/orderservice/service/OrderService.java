package com.project.orderservice.service;

import com.project.orderservice.client.ProductServiceClient;
import com.project.orderservice.dto.*;
import com.project.orderservice.entity.*;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.repository.OrderProductRepository;
import com.project.orderservice.repository.OrderRepository;
import com.project.orderservice.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ShippingRepository shippingRepository;
//    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ProductServiceClient productServiceClient;
//    private final FeignErrorDecoder feignErrorDecoder;
    private final RedissonClient redissonClient;

    /**
     * 구매 가능 시간 & 재고 확인
     *
     * @param orderRequestDto
     * @return
     */
    @Transactional
    public boolean requestOrder(OrderRequestDto orderRequestDto) {
        for (OrderProductRequestDto orderProduct : orderRequestDto.getOrderProducts()) {
            boolean isAvailable = productServiceClient.checkProductForOrder(    // 구매 가능 시간 & 재고 확인
                    orderProduct.getProductId(),
                    orderProduct.getQuantity());

            if (!isAvailable) {
                //PURCHASE_TIME_INVALID, STOCK_INSUFFICIENT 외의 원인
                return false;
            }
        }

        return true;
    }

    /**
     * 주문하기
     *
     * @param memberId
     * @param orderRequestDto
     * @return
     */
    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderRequestDto orderRequestDto) {
        Order savedOrder = null;
        PaymentResponseDto savedPayment = null;

        for (OrderProductRequestDto orderProduct : orderRequestDto.getOrderProducts()) {
            // 1. 재고 수량 감소
            String lockKey = "order_lock_product: " + orderProduct.getProductId();
            RLock lock = redissonClient.getLock(lockKey);

            lock.lock();

            try {
                productServiceClient.reduceStock(orderProduct.getProductId(), orderProduct.getQuantity());
            } finally {
                lock.unlock();
            }
        }

        // 2. 주문 정보 저장
        savedOrder = saveOrder(memberId, orderRequestDto);

        // 3. 결제 처리 및 결제 정보 저장
        savedPayment = paymentService.processPayment(savedOrder);

        if (savedPayment.getStatus() == PaymentStatusEnum.PAYMENT_COMPLETED) {
            // 4-1. 결제 성공
            savedOrder.setStatus(OrderStatusEnum.PAYMENT_COMPLETED);

        } else {
            // 4-2. 결제 실패
            savedOrder.setStatus(OrderStatusEnum.PAYMENT_FAILED);
        }

        // 배송 정보 저장
        saveShipping(savedOrder, orderRequestDto.getShipping());

        // 4-3. 결제 성패여부 저장
        orderRepository.save(savedOrder);

        //트랜잭션 완료
        return new OrderResponseDto(savedOrder);
    }

    /**
     * 주문 정보 저장
     *
     * @param memberId
     * @param orderRequestDto
     * @return
     */
    public Order saveOrder(Long memberId, OrderRequestDto orderRequestDto) {
        List<OrderProduct> orderProductList = new ArrayList<>();
        int totalQuantity = 0;
        int totalPrice = 0;

        Order order = new Order();

        for (OrderProductRequestDto orderProductDto : orderRequestDto.getOrderProducts()) {
            Long productId = orderProductDto.getProductId();
            int quantity = orderProductDto.getQuantity();
            int unitPrice = orderProductDto.getUnitPrice();

            OrderProduct orderProduct = new OrderProduct(unitPrice, quantity, order, productId);
            orderProductList.add(orderProduct);

            totalQuantity += orderProductDto.getQuantity();
            totalPrice += orderProductDto.getUnitPrice() * orderProductDto.getQuantity();
        }

        // 주문 정보 설정
        order.setMemberId(memberId);
        order.setTotalQuantity(totalQuantity);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatusEnum.PAYMENT_PENDING);

        Order saveOrder = orderRepository.save(order);
        log.info("주문 정보 저장 완료");

        List<OrderProduct> orderProducts = orderProductRepository.saveAll(orderProductList);
        log.info("주문 상품 정보 저장 완료");

        return saveOrder;
    }

    /**
     * 결제 실패 시 rollback
     *
     * @param orderProductRequestList
     */
    @Transactional
    public void rollbackStock(List<OrderProductRequestDto> orderProductRequestList) {
        log.info("PAYMENT_FAILED 4");
        for (OrderProductRequestDto orderProduct : orderProductRequestList) {
            String lockKey = "order_lock_product: " + orderProduct.getProductId();
            RLock lock = redissonClient.getLock(lockKey);

            lock.lock();

            try {
                productServiceClient.rollbackStock(orderProduct.getProductId(), orderProduct.getQuantity());
                log.info("PAYMENT_FAILED 5");
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 배송 정보 저장
     *
     * @param order
     * @param shippingRequestDto
     */
    @Transactional
    public void saveShipping(Order order, ShippingRequestDto shippingRequestDto) {
        Shipping shipping = new Shipping();
        shipping.setAddress(shippingRequestDto.getAddress());
        shipping.setAddressDetail(shippingRequestDto.getAddressDetail());
        shipping.setPhone(shippingRequestDto.getPhone());
        shipping.setOrder(order);

        shippingRepository.save(shipping);
        log.info("배송 정보 저장 완료");
    }

    /**
     * 사용자별 전체 주문 내역 조회
     *
     * @param memberId
     * @return
     */
    public List<OrderResponseDto> getOrdersByMemberId(Long memberId) {
        Iterable<Order> orders = orderRepository.findByMemberId(memberId);
        List<Order> orderList = StreamSupport.stream(orders.spliterator(), false)
                .collect(Collectors.toList());

        return orderList.stream()
                .map(order -> new OrderResponseDto(order))
                .collect(Collectors.toList());
    }

    /**
     * 주문 상세 조회
     *
     * @param orderId
     * @return
     */
    public OrderResponseDto getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        OrderResponseDto orderResponseDto = new OrderResponseDto(order);
        return orderResponseDto;
    }

}