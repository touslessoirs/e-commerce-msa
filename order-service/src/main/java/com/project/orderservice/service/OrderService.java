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
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderRequestDto orderRequestDto) {
        log.info("상품 페이지에서 주문 요청");

        Order order = null;

        try {
            //상품 확인 & 재고 감소
            checkAndUpdateStock(orderRequestDto);
            //주문 정보 저장
            order = saveOrder(memberId, orderRequestDto); //PAYMENT_PENDING

            //결제 처리 및 결제 정보 저장
            Payment payment = createPayment(order); //PAYMENT_PENDING
            savePayment(payment);    //PAYMENT_FAILED or PAYMENT_COMPLETED

            //결제 성공 시 배송 정보 저장
            if (payment.getStatus() == PaymentStatusEnum.PAYMENT_COMPLETED) {
                saveShipping(order, orderRequestDto.getShipping());
            }

            //주문 응답 생성 및 반환
            OrderResponseDto orderResponseDto = new OrderResponseDto(order);
            return orderResponseDto;

        } catch (DataIntegrityViolationException e) {
            throw e;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw e;
//            throw new CustomException(ErrorCode.ORDER_FAILED, e);
        }
    }

    /**
     * 재고 rollback
     *
     * @param orderRequestDto
     */
    @Transactional
    public synchronized void rollbackStock(OrderRequestDto orderRequestDto) {
        for (OrderProductRequestDto orderProductDto : orderRequestDto.getOrderProducts()) {
            productServiceClient.updateStock(orderProductDto.getProductId(), orderProductDto.getQuantity());
            log.info("재고 롤백 완료: PRODUCT ID {}", orderProductDto.getProductId());
        }
    }

    /**
     * 상품 확인 & 재고 감소
     *
     * @param orderRequestDto
     */
    @Transactional
    public synchronized void checkAndUpdateStock(OrderRequestDto orderRequestDto) {
        for (OrderProductRequestDto orderProductDto : orderRequestDto.getOrderProducts()) {
            if(productServiceClient.checkProduct(orderProductDto.getProductId(), orderProductDto.getQuantity())){
                productServiceClient.updateStock(orderProductDto.getProductId(), orderProductDto.getQuantity()*(-1));
            } else {
                throw new CustomException(ErrorCode.ORDER_FAILED);
            }
        }
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

            OrderProduct orderProduct= new OrderProduct(unitPrice, quantity, order, productId);
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
     * 배송 정보 저장
     *
     * @param order
     * @param shippingRequestDto
     */
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

//    /**
//     * 결제 및 주문 정보 저장
//     *
//     * @param order
//     * @param payment
//     */
//    public void saveFailureOrder(Order order, Payment payment) {
//        paymentRepository.save(payment);
//        orderRepository.save(order);
//        log.info("결제 실패 정보 저장 완료");
//    }

    /**
     * 사용자별 전체 주문 내역 조회
     *
     * @param memberId
     * @return
     */
    public List<OrderResponseDto> getOrdersByMemberId(Long memberId) {
        Iterable<Order> orders = orderRepository.findByMemberId(memberId);

        //Iterable to List
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
