package com.project.memberservice.feign;

import com.project.memberservice.dto.ProductIdsRequestDto;
import com.project.memberservice.dto.ProductResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /* 상품 상세 조회 */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable Long productId);

    /* 여러 상품의 상세 조회 */
    @PostMapping("/details")
    public ResponseEntity<List<ProductResponseDto>> getProductsDetails(@RequestBody ProductIdsRequestDto productIds);

}