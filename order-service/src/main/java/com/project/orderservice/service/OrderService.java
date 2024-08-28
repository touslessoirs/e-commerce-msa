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
                        FeignErrorDecoder feignErrorDecoder) {
        this.orderRepository = orderRepository;
        this.shippingRepository = shippingRepository;
        this.paymentRepository = paymentRepository;
        this.productServiceClient = productServiceClient;
        this.feignErrorDecoder = feignErrorDecoder;
        this.orderProductRepository = orderProductRepository;
    }

//    @Transactional
//    public OrderResponseDto createOrder(Long memberId, OrderRequestDto orderRequestDto) {
//        List<OrderProduct> orderProductList = new ArrayList<>();
//        int totalQuantity = 0;
//        int totalPrice = 0;
//        Order order = new Order();
//
//        //상품 상세 페이지에서 주문한 경우
//        log.info("상품 상세 페이지에서 주문 요청");
//        for (OrderProductRequestDto orderProductDto : orderRequestDto.getOrderProducts()) {
//            Long productId = orderProductDto.getId();
//
//            //Product 통신 -> 상품 상세 조회
//            ProductResponseDto product = productServiceClient.getProductDetail(productId);
//
//            int quantity = orderProductDto.getQuantity();
//            int price = orderProductDto.getPrice() * quantity;  //해당 상품의 총 가격
//
//            //Product 통신 -> 재고 반영
//            productServiceClient.updateStock(productId, quantity);
//
//            // 주문 상품 정보 생성
//            OrderProduct orderProduct = new OrderProduct(price, quantity, order, productId);
//            orderProductList.add(orderProduct);
//
//            totalQuantity += quantity;
//            totalPrice += price;
//        }
//
//        // 주문 정보 설정
//        order.setMemberId(memberId);
//        order.setTotalQuantity(totalQuantity);
//        order.setTotalPrice(totalPrice);
//
//        // 주문 저장
//        orderRepository.save(order);
//        log.info("주문 정보 저장 완료");
//
//        orderProductRepository.saveAll(orderProductList);
//        log.info("주문 상품 정보 저장 완료");
//
//        // 배송 정보 저장
//        Shipping shipping = new Shipping();
//        shipping.setAddress(orderRequestDto.getShipping().getAddress());
//        shipping.setAddressDetail(orderRequestDto.getShipping().getAddressDetail());
//        shipping.setPhone(orderRequestDto.getShipping().getPhone());
//        shipping.setOrder(order);
//        shippingRepository.save(shipping);
//        log.info("배송 정보 저장 완료");
//
//        OrderResponseDto orderResponseDto = new OrderResponseDto();
//        orderResponseDto.setOrderId(order.getOrderId());
//        orderResponseDto.setTotalPrice(totalPrice);
//        orderResponseDto.setTotalQuantity(totalQuantity);
//        orderResponseDto.setStatus(order.getStatus());
//        orderResponseDto.setMemberId(memberId);
//
//        return orderResponseDto;
//    }

    @Transactional
    public OrderResponseDto createOrder(Long memberId, OrderRequestDto orderRequestDto) {
        log.info("상품 페이지에서 주문 요청");

        Order order = null;

        try {
            //주문 정보 저장
            order = saveOrder(memberId, orderRequestDto); //PAYMENT_PENDING

            //결제 처리 및 결제 정보 저장
            Payment payment = createPayment(order); //PAYMENT_PENDING
            savePayment(payment);    //PAYMENT_FAILED or PAYMENT_COMPLETED

            //결제 성공여부 분기 처리
            if (payment.getStatus() == PaymentStatusEnum.PAYMENT_COMPLETED) {
                //성공 -> 배송 정보 저장
                saveShipping(order, orderRequestDto.getShipping());
            }

            //return OrderResponseDto
            OrderResponseDto orderResponseDto = new OrderResponseDto(order);
            return orderResponseDto;

        } catch (DataIntegrityViolationException e) {
            throw e;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ORDER_FAILED, e);
        }
    }

    /**
     * 주문 실패 시 DB에 주문 정보는 commit, 재고 감소는 rollback
     *
     * @param orderRequestDto
     */
    public void rollbackStock(OrderRequestDto orderRequestDto) {
        for (OrderProductRequestDto orderProductDto : orderRequestDto.getOrderProducts()) {
            // Product 통신 -> 재고 복구
            productServiceClient.updateStock(orderProductDto.getProductId(), orderProductDto.getQuantity());
        }
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

            //Product 통신 -> 상품 상세 조회 (해당 상품 존재하지 않을 경우 예외 발생)
            productServiceClient.getProductDetail(productId);

            //Product 통신 -> 재고 감소
            productServiceClient.updateStock(productId, orderProductDto.getQuantity()*(-1));

            // 주문 상품 정보 생성
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
        log.info("결제 정보 저장 완료");

        orderRepository.save(payment.getOrder());
        log.info("주문 - 결제 상태 업데이트 완료");
    }

    /**
     * 결제 및 주문 정보 저장
     *
     * @param order
     * @param payment
     */
    public void saveFailureOrder(Order order, Payment payment) {
        paymentRepository.save(payment);
        orderRepository.save(order);
        log.info("결제 실패 정보 저장 완료");
    }

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
