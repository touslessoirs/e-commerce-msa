package com.project.orderservice.service;

import com.project.orderservice.client.ProductServiceClient;
import com.project.orderservice.dto.OrderProductRequestDto;
import com.project.orderservice.dto.OrderRequestDto;
import com.project.orderservice.dto.OrderResponseDto;
import com.project.orderservice.dto.ShippingRequestDto;
import com.project.orderservice.entity.*;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.exception.FeignErrorDecoder;
import com.project.orderservice.repository.OrderProductRepository;
import com.project.orderservice.repository.OrderRepository;
import com.project.orderservice.repository.PaymentRepository;
import com.project.orderservice.repository.ShippingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ShippingRepository shippingRepository;
    private final PaymentRepository paymentRepository;
    private final ProductServiceClient productServiceClient;
    private final FeignErrorDecoder feignErrorDecoder;

    public OrderService(OrderRepository orderRepository, OrderProductRepository orderProductRepository,
                        ShippingRepository shippingRepository, PaymentRepository paymentRepository,
                        ProductServiceClient productServiceClient,
                        FeignErrorDecoder feignErrorDecoder
    ) {
        this.orderRepository = orderRepository;
        this.orderProductRepository = orderProductRepository;
        this.shippingRepository = shippingRepository;
        this.paymentRepository = paymentRepository;
        this.productServiceClient = productServiceClient;
        this.feignErrorDecoder = feignErrorDecoder;
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
        // 1. 구매 가능 시간 & 재고 확인 + 재고 감소
        for (OrderProductRequestDto orderProduct : orderRequestDto.getOrderProducts()) {
            boolean isAvailable = productServiceClient.processPurchase(orderProduct.getProductId(), orderProduct.getQuantity()).getBody();

            if (!isAvailable) {
                throw new CustomException(ErrorCode.ORDER_FAILED);
            }
        }

        // 2. 주문 정보 저장
        Order order = saveOrder(memberId, orderRequestDto);

        // 3. 결제 처리 및 결제 정보 저장
        boolean paymentSuccessful = processPayment(order);

        if (paymentSuccessful) {
            // 4. 결제 성공 -> 배송 정보 저장
            saveShipping(order, orderRequestDto.getShipping());
        }

        OrderResponseDto orderResponseDto = new OrderResponseDto(order);
        return orderResponseDto;
    }

    /**
     * 주문 정보 저장
     *
     * @param memberId
     * @param orderRequestDto
     * @return
     */
    @Transactional
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
     * 결제하기
     *
     * @param order
     * @return
     */
    private boolean processPayment(Order order) {
        // 1. 주문 정보를 바탕으로 결제 정보 생성
        Payment payment = createPayment(order);

        // 2. 결제 처리 및 결제 정보 저장
        savePayment(payment);

        // 3. 결제 성공 여부 반환
        return payment.getStatus() == PaymentStatusEnum.PAYMENT_COMPLETED;
    }

    /**
     * 결제 정보 생성
     *
     * @param order
     * @return Payment
     */
    public Payment createPayment(Order order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(PaymentStatusEnum.PAYMENT_PENDING);

        return payment;
    }

    /**
     * 결제 처리 및 결제 정보 저장
     * PAYMENT_PENDING -> PAYMENT_COMPLETED
     * 고객 이탈율 시나리오 : 이탈율 20%
     *
     * @param payment
     * @return
     */
    @Transactional
    public void savePayment(Payment payment) {
        // PAYMENT_PENDING -> PAYMENT_COMPLETED 과정에서 이탈
        // ex) 잔액 부족으로 인한 결제 실패
        if (Math.random() < 0.20) { //20%
            payment.setStatus(PaymentStatusEnum.PAYMENT_FAILED);
            payment.getOrder().setStatus(OrderStatusEnum.PAYMENT_FAILED);
        } else {
            payment.setStatus(PaymentStatusEnum.PAYMENT_COMPLETED);
            payment.getOrder().setStatus(OrderStatusEnum.PAYMENT_COMPLETED);
        }

        paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());
        log.info("결제 정보 저장 완료");
        log.info(String.valueOf(payment.getOrder().getStatus()));
    }

    /**
     * 결제 실패 -> 재고 rollback
     *
     * @param orderProducts
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackStock(List<OrderProductRequestDto> orderProducts) {
        for (OrderProductRequestDto orderProduct : orderProducts) {
            productServiceClient.rollbackStock(orderProduct.getProductId(), orderProduct.getQuantity());
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
