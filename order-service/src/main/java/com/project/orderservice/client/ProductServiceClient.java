package com.project.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /* 상품 상세 조회 */
//    @GetMapping("/{productId}")
//    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable("productId") Long productId);

    /* 재고 수량 & 구매 가능 시간 확인 & 재고 감소 */
//    @PostMapping("/check-product")
//    public ResponseEntity<Boolean> processPurchase(@RequestParam Long productId, @RequestParam int quantity);

    /* 구매 가능 시간 & 재고 확인 */
    @GetMapping("/check-product")
    public Boolean checkProductForOrder(@RequestParam Long productId, @RequestParam int quantity);

    /* 재고 수량 감소 */
    @PostMapping("/reduce-stock")
    public ResponseEntity reduceStock(@RequestParam Long productId, @RequestParam int quantity);

//    /* 구매 가능 시간 확인 */
//    @GetMapping("/check-purchase-time")
//    public ResponseEntity<Boolean> isProductPurchasable(@RequestParam Long productId);
//
//    /* 재고 수량 확인 & 재고 감소 */
//    @PostMapping("/check-and-update-stock")
//    public ResponseEntity<Boolean> checkAndUpdateStock(@RequestParam Long productId, @RequestParam int quantity);

    /* 주문 실패 시 롤백 */
    @PostMapping("/rollback-stock")
    public ResponseEntity rollbackStock(@RequestParam Long productId, @RequestParam int quantity);





}