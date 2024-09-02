package com.project.orderservice.service;

import com.project.orderservice.client.ProductServiceClient;
import com.project.orderservice.dto.*;
import com.project.orderservice.entity.*;
import com.project.orderservice.event.PaymentRequestEvent;
import com.project.orderservice.event.PaymentResponseEvent;
import com.project.orderservice.event.ShippingRequestEvent;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.exception.FeignErrorDecoder;
import com.project.orderservice.repository.OrderProductRepository;
import com.project.orderservice.repository.OrderRepository;
import com.project.orderservice.repository.PaymentRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

//    private static final String ORDER_REQUEST_TOPIC = "order-request-topic";
//    private static final String TEST_TOPIC = "test-topic";
//    private static final String TEST_TOPIC_2 = "test-topic-2";
    private static final String PAYMENT_REQUEST_TOPIC = "payment-request-topic";
    private static final String PAYMENT_RESPONSE_TOPIC = "payment-response-topic";
    private static final String SHIPPING_TOPIC = "shipping-topic";
    private static final String ROLLBACK_TOPIC = "rollback-topic";

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ShippingRepository shippingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ProductServiceClient productServiceClient;
    private final FeignErrorDecoder feignErrorDecoder;
    private final RedissonClient redissonClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConcurrentHashMap<Long, CompletableFuture<Boolean>> pendingResponses = new ConcurrentHashMap<>();

//    /**
//     * kafka event 발행
//     *
//     * @param memberId
//     * @param orderRequest
//     */
//    public void produceEvent(Long memberId, FlashSaleRequestDto orderRequest) {
//        OrderProductRequestDto orderProductRequestDto = orderRequest.getOrderProduct();
//
//        CompletableFuture<Boolean> future = new CompletableFuture<>();
//        pendingResponses.put(orderProductRequestDto.getProductId(), future);
//
//        sendEvent(TEST_TOPIC,
//                new OrderRequestEvent(
//                        orderProductRequestDto.getProductId(),
//                        orderProductRequestDto.getQuantity()
//                )
//        );
//    }
//
//    private void sendEvent(String topic, Object event) {
//        kafkaTemplate.send(topic, event);
//    }
//
//    /**
//     * kafka event 수신
//     *
//     * @param isAvailable
//     */
//    @KafkaListener(topics = TEST_TOPIC_2)
//    public void listenEvent(Boolean isAvailable) {
//        log.info("Received OrderRequestEvent: isAvailable: {}", isAvailable);
//    }
//
    /**
     * 구매 가능 시간 & 재고 확인
     *
     * @param flashSaleRequestDto
     * @return
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
     * @param memberId
     * @param flashSaleRequestDto
     * @return
     */
    @Transactional
    public OrderResponseDto createOrder(Long memberId, FlashSaleRequestDto flashSaleRequestDto) {
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

    @KafkaListener(topics = PAYMENT_RESPONSE_TOPIC)
    public void handlePaymentResultEvent(PaymentResponseEvent event) {

        try {
            if (event.order() == null) {
                throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
            }

            // order status update
            log.info("{} listen", event.status());
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
     * @param flashSaleRequestDto
     * @return
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
     * @param orderProductDto
     */
    @Transactional
    public void rollbackStock(OrderProductRequestDto orderProductDto) {
        log.info("rollback : {}", orderProductDto.getProductId());

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
     * 배송 정보 저장
     *
     * @param event
     */
    @KafkaListener(topics = SHIPPING_TOPIC)
    public void saveShipping(ShippingRequestEvent event) {

        log.info("address : "+event.address());

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
