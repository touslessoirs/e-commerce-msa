package com.project.orderservice.client;

import com.project.orderservice.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable("productId") Long productId);

    @PutMapping("/{productId}")
    public void rollbackStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity);

//    @GetMapping("/check-product/{productId}")
//    public boolean isProductPurchasable(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity);

//    @GetMapping("/check-product/{productId}")
//    public void checkAndUpdateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity);



    /* 구매 가능 시간 확인 */
    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> isProductAvailable(@RequestParam Long productId);

    /* 재고 수량 확인 & 재고 감소 */
    @PostMapping("/check-and-update-stock")
    public ResponseEntity<Boolean> checkAndUpdateStock(@RequestParam Long productId, @RequestParam int quantity);

    /* 주문 시 성패여부 분기처리 */
    @PostMapping("/update-stock")
    public ResponseEntity<Void> updateStock(@RequestParam Long productId,
                                            @RequestParam int quantity,
                                            @RequestParam boolean success);



}