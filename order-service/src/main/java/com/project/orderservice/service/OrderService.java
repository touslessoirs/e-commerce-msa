package com.project.orderservice.service;

import com.project.orderservice.feign.CartServiceClient;
import com.project.orderservice.feign.ProductOrderFlowServiceClient;
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
    private final ProductOrderFlowServiceClient productOrderFlowServiceClient;
    private final CartServiceClient cartServiceClient;
    private final RedissonClient redissonClient;
    private final FeignErrorDecoder feignErrorDecoder;

    /**
     * 주문 가능 여부 확인
     *
     * 1. 해당 상품이 현재 구매 가능한 상품인지 확인
     * 2. 해당 상품의 재고가 구매하려는 수량 이상인지 확인
     *
     * @param orderRequestDto 주문 요청에 필요한 정보 (상품 목록, 수량, 가격 등)
     * @return 주문하고자 하는 상품들이 모두 주문 가능하면 true
     *         아래 에러 발생 가능 상황이 아닌 다른 사유로 주문 불가능하면 false
     *
     * Error 발생 가능 상황:
     * 1. 구매 가능 시간이 아닐 때 (PURCHASE_TIME_INVALID)
     * 2. 재고가 부족할 때 (STOCK_INSUFFICIENT)
     */
    @Transactional
    public boolean requestOrder(OrderRequestDto orderRequestDto) {
        for (OrderProductRequestDto orderProduct : orderRequestDto.getOrderProducts()) {
            boolean isAvailable = productOrderFlowServiceClient.checkProductForOrder(    // 구매 가능 시간 & 재고 확인
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
     * 주문 생성 및 결제 요청
     *
     * 주문 요청에 따라 주문을 생성하고, 각 상품의 재고를 감소시킨 후 결제 처리를 진행한다.
     * 결제가 성공하면 'PAYMENT_COMPLETED', 실패하면 'PAYMENT_FAILED'로 설정한다.
     * 해당 주문에 해당하는 배송 정보를 저장한다.
     * 만약 주문이 장바구니를 통해 이루어진 경우, 해당 상품(들)을 장바구니에서 삭제한다.
     *
     * @param id 주문을 요청한 회원의 ID
     * @param orderRequestDto 주문 요청에 필요한 정보 (상품 목록, 수량, 가격 등)
     * @return 저장된 주문 정보 (배송 정보, 결제 처리 결과 등 포함)
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
                    productOrderFlowServiceClient.reduceStock(orderProduct.getProductId(), orderProduct.getQuantity());
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
                // 장바구니에 담긴 상품 삭제
                List<Long> productIds = orderRequestDto.getOrderProducts().stream()
                        .map(OrderProductRequestDto::getProductId)
                        .collect(Collectors.toList());
                cartServiceClient.deleteProductsFromCart(id, productIds);
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
     * 각 상품의 수량 및 가격을 계산하여 총 주문 수량과 금액을 설정한 후 데이터베이스에 저장한다.
     * 각 상품에 대한 주문 정보도 별도로 데이터베이스에 저장되어 연결된 상품 목록을 함께 관리할 수 있도록 한다.
     *
     * @param id 주문을 요청한 회원의 ID
     * @param orderRequestDto 주문 요청에 필요한 정보 (상품 목록, 수량, 가격 등)
     * @return 저장된 주문 정보 (배송 정보, 결제 처리 결과 등 미포함)
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
     * 재고 롤백 처리
     *
     * 주문 실패, 결제 실패, 주문 취소 또는 반품 승인 시 해당 상품의 재고를 롤백한다.
     *
     * @param orderProductRequestList 재고를 롤백해야 하는 상품 목록
     */
    @Transactional
    public void rollbackStock(List<OrderProductRequestDto> orderProductRequestList) {
        for (OrderProductRequestDto orderProduct : orderProductRequestList) {
            String lockKey = "order_lock_product: " + orderProduct.getProductId();
            RLock lock = redissonClient.getLock(lockKey);

            lock.lock();

            try {
                productOrderFlowServiceClient.rollbackStock(orderProduct.getProductId(), orderProduct.getQuantity());
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 배송 정보 저장
     *
     * 주어진 주문과 연관된 배송 정보를 데이터베이스에 저장한다.
     *
     * @param order 저장하려는 배송 정보와 연결된 주문 객체
     * @param shippingRequestDto 저장된 배송 정보
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
     * 주어진 사용자 ID에 해당하는 모든 주문 내역을 조회하여, 각 주문을 DTO 객체로 변환한 리스트로 반환한다.
     *
     * @param id 주문을 조회할 회원의 ID
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
     * @param id 해당 주문을 조회할 회원의 ID
     * @param orderId 상세 정보를 조회할 주문의 ID
     * @return 해당 주문의 상세 정보
     */
    public OrderResponseDto getOrderDetail(String id, Long orderId) {
        Long memberId = Long.parseLong(id);

        Order order = orderRepository.findByOrderIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        return new OrderResponseDto(order);
    }

    /**
     * 주문 취소
     *
     * 결제가 완료된 상태인(PAYMENT_COMPLETED) 주문만 취소할 수 있으며, 승인 절차 없이 즉시 취소 처리함
     * 주문 취소 후 해당 주문에 대한 재고는 롤백한다.
     *
     * @param id 취소를 요청한 회원의 ID
     * @param orderId 취소하려는 주문의 ID
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
     * 
     * 배송 완료 상태(DELIVERED)인 주문만 반품 신청이 가능하며, 관리자의 별도의 승인 절차가 필요함
     * 주문 상태를 '반품 요청(RETURN_REQUESTED)'으로 변경한다.
     *
     * @param id 반품을 요청한 회원의 ID
     * @param orderId 반품하려는 주문의 ID
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
     * 
     * 반품 신청 상태(RETURN_REQUESTED)인 주문에 대해 반품을 승인 처리한다.
     * 주문 상태를 '반품 완료(RETURN_COMPLETED)'으로 변경한다.
     * 반품 승인 후 해당 주문에 대한 재고는 롤백한다.
     *
     * @param orderId 반품을 승인할 주문의 ID
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