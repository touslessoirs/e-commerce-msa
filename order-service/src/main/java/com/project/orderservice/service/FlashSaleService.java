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
import com.project.orderservice.feign.ProductOrderFlowServiceClient;
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
    private final ProductOrderFlowServiceClient productOrderFlowServiceClient;
    private final RedissonClient redissonClient;
    private final FeignErrorDecoder feignErrorDecoder;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 주문 가능 여부 확인
     *
     * 1. 해당 상품이 현재 구매 가능한 상품인지 확인
     * 2. 해당 상품의 재고가 구매하려는 수량 이상인지 확인
     *
     * @param flashSaleRequestDto 주문 정보
     * @return 주문하고자 하는 상품이 주문 가능하면 true
     *         아래 에러 발생 가능 상황이 아닌 다른 사유로 주문 불가능하면 false
     *
     * Error 발생 가능 상황:
     * 1. 구매 가능 시간이 아닐 때 (PURCHASE_TIME_INVALID)
     * 2. 재고가 부족할 때 (STOCK_INSUFFICIENT)
     */
    @Transactional
    public boolean requestOrder(FlashSaleRequestDto flashSaleRequestDto) {
        OrderProductRequestDto orderProduct = flashSaleRequestDto.getOrderProduct();
        boolean isAvailable = productOrderFlowServiceClient.checkProductForOrder(    // 구매 가능 시간 & 재고 확인
                orderProduct.getProductId(),
                orderProduct.getQuantity());

        if (!isAvailable) {
            //PURCHASE_TIME_INVALID, STOCK_INSUFFICIENT 외의 원인
            return false;
        }

        return true;
    }

    /**
     * 주문 생성 및 결제 요청
     *
     * 주문 요청에 따라 주문을 생성하고, 각 상품의 재고를 감소시킨 후 주문 정보를 반환한다.
     * 결제 처리 및 배송 정보 저장은 별도의 이벤트 요청을 보내 비동기 처리한다.
     * 해당 주문에 해당하는 배송 정보를 저장한다.
     *
     * @param id 주문을 요청한 회원의 ID
     * @param flashSaleRequestDto 주문 요청에 필요한 정보 (상품 목록, 수량, 가격 등)
     * @return 저장된 주문 정보 (배송 정보, 결제 처리 결과 등 포함)
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
            productOrderFlowServiceClient.reduceStock(orderProduct.getProductId(), orderProduct.getQuantity());
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
     * Kafka를 통해 결제 완료 이벤트를 수신하고, 결제 성공 여부에 따라 주문 상태를 업데이트한다.
     * 결제가 성공하면 'PAYMENT_COMPLETED', 실패하면 'PAYMENT_FAILED'로 설정한다.
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
     * 각 상품의 수량 및 가격을 계산하여 총 주문 수량과 금액을 설정한 후 데이터베이스에 저장한다.
     * 각 상품에 대한 주문 정보도 별도로 데이터베이스에 저장되어 연결된 상품 목록을 함께 관리할 수 있도록 한다.
     *
     * @param memberId 주문을 요청한 회원의 ID
     * @param flashSaleRequestDto 주문 요청에 필요한 정보 (상품 목록, 수량, 가격 등)
     * @return 저장된 주문 정보 (배송 정보, 결제 처리 결과 등 미포함)
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
     * 재고 롤백 처리
     *
     * 주문 실패, 결제 실패, 주문 취소 또는 반품 승인 시 해당 상품의 재고를 롤백한다.
     *
     * @param orderProductDto 재고를 롤백해야 하는 상품
     */
    @Transactional
    public void rollbackStock(OrderProductRequestDto orderProductDto) {
        String lockKey = "order_lock_product: " + orderProductDto.getProductId();
        RLock lock = redissonClient.getLock(lockKey);

        lock.lock();

        try {
            productOrderFlowServiceClient.rollbackStock(orderProductDto.getProductId(), orderProductDto.getQuantity());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 배송 정보 저장 이벤트 수신
     *
     * Kafka를 통해 배송 정보 저장 이벤트를 수신하고, 해당 정보를 데이터베이스에 저장한다.
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
