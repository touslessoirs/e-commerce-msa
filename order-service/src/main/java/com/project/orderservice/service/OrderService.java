package com.project.orderservice.service;

import com.project.orderservice.client.CartServiceClient;
import com.project.orderservice.client.ProductServiceClient;
import com.project.orderservice.dto.*;
import com.project.orderservice.entity.*;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.exception.FeignErrorDecoder;
import com.project.orderservice.repository.OrderProductRepository;
import com.project.orderservice.repository.OrderRepository;
import com.project.orderservice.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataAccessException;
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
    private final PaymentService paymentService;
    private final ProductServiceClient productServiceClient;
    private final CartServiceClient cartServiceClient;
    private final RedissonClient redissonClient;
    private final FeignErrorDecoder feignErrorDecoder;

    /**
     * 구매 가능 시간 & 재고 확인
     *
     * @param orderRequestDto 주문 정보
     * @return 주문하고자 하는 상품들이 모두 주문 가능한지 여부
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
     * @param id memberId
     * @param orderRequestDto 주문 정보
     * @return 완료된 주문
     */
    @Transactional
    public OrderResponseDto createOrder(String id, OrderRequestDto orderRequestDto) {
        Order savedOrder = null;
        PaymentResponseDto savedPayment = null;

        try {
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
            savedOrder = saveOrder(id, orderRequestDto);

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

            if (orderRequestDto.isFromCart()) {
                // 장바구니 삭제
                List<Long> productIds = orderRequestDto.getOrderProducts().stream()
                        .map(OrderProductRequestDto::getProductId)
                        .collect(Collectors.toList());
                log.info("id: {}, productIds size: {}", id, productIds.size());
                cartServiceClient.deleteProductFromCart(id, productIds);
            }

            //트랜잭션 완료
            return new OrderResponseDto(savedOrder);

        } catch (DataAccessException e) {
            throw e;
        }
    }

    /**
     * 주문 정보 저장
     *
     * @param id memberId
     * @param orderRequestDto 주문 정보
     * @return 저장 완료한 주문
     */
    public Order saveOrder(String id, OrderRequestDto orderRequestDto) {
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
        order.setMemberId(Long.parseLong(id));
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
     * 재고 rollback (주문 실패, 결제 실패, 주문 취소, 반품 승인)
     *
     * @param orderProductRequestList rollback해야하는 상품 목록
     */
    @Transactional
    public void rollbackStock(List<OrderProductRequestDto> orderProductRequestList) {
        for (OrderProductRequestDto orderProduct : orderProductRequestList) {
            String lockKey = "order_lock_product: " + orderProduct.getProductId();
            RLock lock = redissonClient.getLock(lockKey);

            lock.lock();

            try {
                productServiceClient.rollbackStock(orderProduct.getProductId(), orderProduct.getQuantity());
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 배송 정보 저장
     *
     * @param order 저장하려는 배송 건에 해당하는 주문
     * @param shippingRequestDto 배송 정보
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
     * @param id memberId
     * @return 해당 사용자의 전체 주문 내역
     */
    public List<OrderResponseDto> getOrdersByMemberId(String id) {
        //Order -> OrderResponseDto -> add list
        return StreamSupport.stream(orderRepository.findByMemberId(Long.parseLong(id)).spliterator(), false)
                .map(OrderResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 주문 상세 조회
     *
     * @param orderId
     * @return 해당 주문의 상세 정보
     */
    public OrderResponseDto getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        return new OrderResponseDto(order);
    }

    /**
     * 주문 취소
     * PAYMENT_COMPLETED 상태인 주문만 취소 가능 (=결제 완료 1일 이내)
     * 승인 절차 없이 즉시 취소 처리한다.
     * 주문 취소 후 재고 rollback
     *
     * @param orderId
     */
    public void cancelOrder(String id, Long orderId) {
        Long memberId = Long.parseLong(id);

        Order order = orderRepository.findByOrderIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatusEnum.PAYMENT_COMPLETED) {
            throw new CustomException(ErrorCode.CANCELLATION_NOT_ALLOWED);
        }

        order.setStatus(OrderStatusEnum.CANCELLED);
        orderRepository.save(order);

        // Order -> OrderProduct -> OrderProductRequestDto
        List<OrderProduct> orderProducts = orderProductRepository.findAllByOrderOrderId(orderId);
        List<OrderProductRequestDto> orderProductRequestDtoList = orderProducts.stream()
                .map(OrderProductRequestDto::new)
                .collect(Collectors.toList());

        // 재고 rollback
        rollbackStock(orderProductRequestDtoList);

        log.info("주문 취소 완료 - orderId: {}", orderId);
    }

    /**
     * 반품 신청
     * DELIVERED 상태인 주문만 반품 신청 가능 (=배송 완료 3일 이내)
     *
     * @param orderId
     */
    public void requestReturn(String id, Long orderId) {
        Long memberId = Long.parseLong(id);

        Order order = orderRepository.findByOrderIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatusEnum.DELIVERED) {
            throw new CustomException(ErrorCode.RETURN_NOT_ALLOWED);
        }

        order.setStatus(OrderStatusEnum.RETURN_REQUESTED);
        orderRepository.save(order);

        log.info("반품 신청 완료 - orderId: {}", orderId);
    }

    /**
     * 반품 신청 승인
     * RETURN_REQUESTED 상태인 주문만 반품 승인 가능
     * 반품 승인 후 재고 rollback
     *
     * @param orderId
     */
    @Transactional
    public void approveReturnRequest(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatusEnum.RETURN_REQUESTED) {
            throw new CustomException(ErrorCode.ORDER_IS_NOT_RETURN_REQUESTED);
        }

        order.setStatus(OrderStatusEnum.RETURN_COMPLETED);
        orderRepository.save(order);

        // Order -> OrderProduct -> OrderProductRequestDto
        List<OrderProduct> orderProducts = orderProductRepository.findAllByOrderOrderId(orderId);
        List<OrderProductRequestDto> orderProductRequestDtoList = orderProducts.stream()
                .map(OrderProductRequestDto::new)
                .collect(Collectors.toList());

        // 재고 rollback
        rollbackStock(orderProductRequestDtoList);

        log.info("반품 신청 승인 완료 - orderId: {}", orderId);
    }

}