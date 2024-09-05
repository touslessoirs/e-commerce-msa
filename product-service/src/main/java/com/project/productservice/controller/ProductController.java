package com.project.productservice.controller;

import com.project.productservice.dto.ProductIdsRequestDto;
import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class ProductController {

    @Value("${greeting.message}")
    private String greeting;

    @Value("${server.port}")
    private String port;

    private final ProductService productService;

    @GetMapping("/health-check")
    public String status() {
        return String.format("PRODUCT SERVICE Running on PORT %s", port);
    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting;
    }

    /* 전체 상품 조회 */
    @GetMapping("/allProducts")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        Iterable<ProductResponseDto> productList = productService.getAllProducts();

        List<ProductResponseDto> result = new ArrayList<>();
        productList.forEach(result::add);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 상품 상세 조회 */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable Long productId) {
        log.info("getProductDetail 호출");
        ProductResponseDto productResponseDto = productService.getProductDetail(productId);
        return ResponseEntity.ok(productResponseDto);
    }

    /* 여러 상품의 상세 조회 */
    @PostMapping("/products/details")
    public ResponseEntity<List<ProductResponseDto>> getProductsDetails(@RequestBody ProductIdsRequestDto productIds) {
        List<ProductResponseDto> productDetails = productService.getProductsDetails(productIds);
        return ResponseEntity.ok(productDetails);
    }

}