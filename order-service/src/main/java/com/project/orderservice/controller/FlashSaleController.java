package com.project.orderservice.controller;

import com.project.orderservice.dto.FlashSaleRequestDto;
import com.project.orderservice.dto.OrderResponseDto;
import com.project.orderservice.exception.CustomException;
import com.project.orderservice.exception.ErrorCode;
import com.project.orderservice.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    /* 주문 요청 - 주문 가능 여부 확인 및 주문하기 */
    @PostMapping("/{memberId}/flash-sale/request")
    public ResponseEntity<OrderResponseDto> requestOrder(@PathVariable("memberId") Long memberId,
                                                         @Valid @RequestBody FlashSaleRequestDto flashSaleRequestDto) {

        // 구매 가능 시간 & 재고 확인
        boolean isAvailable = flashSaleService.requestOrder(flashSaleRequestDto);

        if (isAvailable) {
            log.info("주문 가능");

            OrderResponseDto orderResponseDto = flashSaleService.createOrder(memberId, flashSaleRequestDto);

            log.info(String.valueOf(orderResponseDto.getStatus()));
//            if (orderResponseDto.getStatus() == OrderStatusEnum.PAYMENT_COMPLETED) {
////                log.info("결제 성공");
                return ResponseEntity.status(HttpStatus.CREATED).body(orderResponseDto);
//            } else {
////                log.info("결제 실패");
////                flashSaleService.rollbackStock(flashSaleRequestDto.getOrderProduct());
//                if (orderResponseDto.getStatus() == OrderStatusEnum.PAYMENT_FAILED) {
//                    throw new CustomException(ErrorCode.PAYMENT_FAILED);
//                } else {
////                    log.info("주문 실패");
//                    throw new CustomException(ErrorCode.ORDER_FAILED);
//                }
//            }

        } else {
            //주문 불가능 -> 클라이언트 응답 반환
            log.info("주문 불가능");
            throw new CustomException(ErrorCode.ORDER_REQUEST_DENIED);
        }
    }

}