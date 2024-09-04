package com.project.orderservice.service;

import com.project.orderservice.dto.FlashSaleRequestDto;
import com.project.orderservice.dto.OrderProductRequestDto;
import com.project.orderservice.dto.OrderResponseDto;
import com.project.orderservice.dto.PaymentResponseDto;
import com.project.orderservice.entity.*;
import com.project.orderservice.event.PaymentRequestEvent;
import com.project.orderservice.event.PaymentResponseEvent;
import com.project.orderservice.event.ShippingRequestEvent;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.exception.FeignErrorDecoder;
import com.project.orderservice.feign.ProductServiceClient;
import com.project.orderservice.repository.OrderProductRepository;
import com.project.orderservice.repository.OrderRepository;
import com.project.orderservice.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private static final String PAYMENT_REQUEST_TOPIC = "payment-request-topic";
    private static final String PAYMENT_RESPONSE_TOPIC = "payment-response-topic";
    private static final String SHIPPING_TOPIC = "shipping-topic";

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ShippingRepository shippingRepository;
    private final ProductServiceClient productServiceClient;
    private final RedissonClient redissonClient;
    private final FeignErrorDecoder feignErrorDecoder;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 구매 가능 시간 & 재고 확인
     *
     * @param flashSaleRequestDto 주문 정보
     * @return 주문하고자 하는 상품이 주문 가능한지 여부
     */
    @Transactional
    public boolean requestOrder(FlashSaleRequestDto flashSaleRequestDto) {
        OrderProductRequestDto orderProduct = flashSaleRequestDto.getOrderProduct();
        boolean isAvailable = productServiceClient.checkProductForOrder(    // 구매 가능 시간 & 재고 확인
                orderProduct.getProductId(),
                orderProduct.getQuantity());

        if (!isAvailable) {
            //PURCHASE_TIME_INVALID, STOCK_INSUFFICIENT 외의 원인
            return false;
        }

        return true;
    }

    /**
     * 주문하기
     *
     * @param id memberId
     * @param flashSaleRequestDto 주문 정보
     * @return 완료된 주문
     */
    @Transactional
    public OrderResponseDto createOrder(String id, FlashSaleRequestDto flashSaleRequestDto) {
        Long memberId = Long.parseLong(id);

        Order savedOrder = null;
        PaymentResponseDto savedPayment = null;

        OrderProductRequestDto orderProduct = flashSaleRequestDto.getOrderProduct();
        // 1. 재고 수량 감소
        String lockKey = "order_lock_product: " + orderProduct.getProductId();
        RLock lock = redissonClient.getLock(lockKey);

        lock.lock();

        try {
            productServiceClient.reduceStock(orderProduct.getProductId(), orderProduct.getQuantity());
        } finally {
            lock.unlock();
        }

        // 2. 주문 정보 저장
        savedOrder = saveOrder(memberId, flashSaleRequestDto);

        // 결제 요청 event send
        kafkaTemplate.send(PAYMENT_REQUEST_TOPIC, new PaymentRequestEvent(savedOrder, orderProduct));

        // 배송 정보 저장 event send
        ShippingRequestEvent shippingEvent = new ShippingRequestEvent(
                flashSaleRequestDto.getShipping().getAddress(),
                flashSaleRequestDto.getShipping().getAddressDetail(),
                flashSaleRequestDto.getShipping().getPhone(),
                savedOrder
        );
        kafkaTemplate.send(SHIPPING_TOPIC, shippingEvent);

        // 트랜잭션 완료
        return new OrderResponseDto(savedOrder);
    }

    /**
     * 결제 완료 이벤트 수신
     * 
     * @param event 결제 완료 정보
     */
    @KafkaListener(topics = PAYMENT_RESPONSE_TOPIC)
    public void handlePaymentResultEvent(PaymentResponseEvent event) {

        try {
            if (event.order() == null) {
                throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
            }

            // order status update
            Order paidOrder = event.order();
            if (event.status() == PaymentStatusEnum.PAYMENT_COMPLETED) {
                // 결제 성공
                log.info("결제 성공");
                paidOrder.setStatus(OrderStatusEnum.PAYMENT_COMPLETED);
                orderRepository.save(paidOrder);

            } else {
                // 결제 실패
                log.info("결제 실패");
                paidOrder.setStatus(OrderStatusEnum.PAYMENT_FAILED);
                orderRepository.save(paidOrder);

                rollbackStock(event.orderProductRequestDto());
            }
        } catch (Exception e) {
            log.error("결제 결과 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 주문 정보 저장
     *
     * @param memberId
     * @param flashSaleRequestDto 주문 정보
     * @return 저장 완료한 주문
     */
    public Order saveOrder(Long memberId, FlashSaleRequestDto flashSaleRequestDto) {
        OrderProductRequestDto orderProductDto = flashSaleRequestDto.getOrderProduct();

        int totalQuantity = orderProductDto.getQuantity();
        int totalPrice = orderProductDto.getUnitPrice() * totalQuantity;

        Order order = new Order();

        // 주문 정보 설정
        order.setMemberId(memberId);
        order.setTotalQuantity(totalQuantity);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatusEnum.PAYMENT_PENDING);

        Order savedOrder = orderRepository.save(order);
        log.info("주문 정보 저장 완료");

        List<OrderProduct> orderProductList = new ArrayList<>();
        OrderProduct orderProduct = new OrderProduct(
                orderProductDto.getUnitPrice(),
                orderProductDto.getQuantity(),
                savedOrder,
                orderProductDto.getProductId()
        );
        orderProductList.add(orderProduct);

        List<OrderProduct> orderProducts = orderProductRepository.saveAll(orderProductList);
        log.info("주문 상품 정보 저장 완료");

        return savedOrder;
    }

    /**
     * 결제 실패 시 rollback
     *
     * @param orderProductDto rollback해야하는 상품 정보
     */
    @Transactional
    public void rollbackStock(OrderProductRequestDto orderProductDto) {
        String lockKey = "order_lock_product: " + orderProductDto.getProductId();
        RLock lock = redissonClient.getLock(lockKey);

        lock.lock();

        try {
            productServiceClient.rollbackStock(orderProductDto.getProductId(), orderProductDto.getQuantity());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 배송 정보 저장 이벤트 수신
     *
     * @param event 배송 정보
     */
    @KafkaListener(topics = SHIPPING_TOPIC)
    public void saveShipping(ShippingRequestEvent event) {
        Shipping shipping = new Shipping(
                event.address(),
                event.addressDetail(),
                event.phone(),
                event.order()
        );
        shippingRepository.save(shipping);
        log.info("배송 정보 저장 완료");
    }
}
