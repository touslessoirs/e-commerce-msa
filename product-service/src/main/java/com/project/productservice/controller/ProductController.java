package com.project.productservice.controller;

import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/")
public class ProductController {

    @Value("${greeting.message}")
    private String greeting;
    private final Environment env;
    private final ProductService productService;

    public ProductController(Environment env, ProductService memberService) {
        this.env = env;
        this.productService = memberService;
    }

    @GetMapping("/health-check")
    public String status() {
        return String.format("PRODUCT SERVICE Running on PORT %s", env.getProperty("server.port"));

    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting;
    }

    /* 전체 상품 조회 */
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        Iterable<ProductResponseDto> productList = productService.getAllProducts();

        List<ProductResponseDto> result = new ArrayList<>();
        productList.forEach(result::add);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 상품 상세 조회 */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable Long productId) {
        log.info("getProductDetail 호출");
        ProductResponseDto productResponseDto = productService.getProductDetail(productId);
        return ResponseEntity.ok(productResponseDto);
    }

    /* 재고 수량 & 구매 가능 시간 확인 */
    @GetMapping("/check-product/{productId}")
    public boolean checkProduct(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity) {
        log.info("checkProduct 호출");
        return productService.checkProduct(productId, quantity);
    }

    /* 재고 수량 변경 */
    @PutMapping("/{productId}")
    public void updateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity) {
        log.info("updateStock 호출");
        productService.updateStock(productId, quantity);
    }

}