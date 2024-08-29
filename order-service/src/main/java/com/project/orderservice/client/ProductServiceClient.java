package com.project.orderservice.client;

import com.project.orderservice.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable("productId") Long productId);

    @PutMapping("/{productId}")
    public void updateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity);

    @GetMapping("/check-product/{productId}")
    public boolean isProductPurchasable(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity);
}