package com.project.productservice.controller;

import com.project.productservice.service.ProductOrderFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 주문과 관련된 상품 관리 기능을 제공하는 Controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/order-flow")
public class ProductOrderFlowController {

    private final ProductOrderFlowService productOrderFlowService;

    /*
    * 주문 가능 여부 확인 (주문 요청 시)
    * 구매 가능 시간 & 재고 수량 확인
    * */
    @GetMapping("/check-product")
    public boolean checkProductForOrder(@RequestParam Long productId, @RequestParam int quantity) {
        boolean isAvailable = productOrderFlowService.checkProductForOrder(productId, quantity);
        return isAvailable;
    }

    /* 구매 가능 시간 확인 */
    @GetMapping("/check-purchase-time")
    public ResponseEntity<Boolean> validatePurchaseTime(@RequestParam Long productId) {
        boolean isAvailable = productOrderFlowService.checkPurchaseTime(productId);
        return ResponseEntity.ok(isAvailable);
    }

    /* 재고 수량(주문 가능한 재고량인지) 확인 */
    @GetMapping("/check-stock")
    public ResponseEntity<Boolean> checkStock(@RequestParam Long productId, @RequestParam int quantity) {
        boolean isStockUpdated = productOrderFlowService.checkStock(productId, quantity);
        return ResponseEntity.ok(isStockUpdated);
    }

    /* 재고 수량 감소 */
    @PostMapping("/reduce-stock")
    public ResponseEntity reduceStock(@RequestParam Long productId, @RequestParam int quantity) {
        productOrderFlowService.reduceStock(productId, quantity);
        return ResponseEntity.ok("재고 감소 처리가 완료되었습니다.");
    }

    /* 재고 수량 증가 (주문 실패 시) */
    @PostMapping("/rollback-stock")
    public ResponseEntity rollbackStock(@RequestParam Long productId, @RequestParam int quantity) {
        productOrderFlowService.rollbackStock(productId, quantity);
        return ResponseEntity.ok("재고 증가 처리가 완료되었습니다.");
    }
}
