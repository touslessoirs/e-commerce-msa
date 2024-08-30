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
//    @GetMapping("/check-product/{productId}")
//    public boolean isProductPurchasable(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity) {
//        log.info("isProductPurchasable 호출");
//        return productService.isProductPurchasable(productId, quantity);
//    }

//    /* 재고 수량 변경 */
//    @PutMapping("/{productId}")
//    public void rollbackStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity) {
//        log.info("rollbackStock 호출");
//        productService.rollbackStock(productId, quantity);
//    }

    /* 재고 수량 & 구매 가능 시간 확인 & 재고 감소 */
//    @GetMapping("/check-product/{productId}")
//    public void checkAndUpdateStock(@PathVariable("productId") Long productId, @RequestParam("quantity") int quantity) {
//        log.info("checkAndUpdateStock 호출");
//        productService.checkAndUpdateStock(productId, quantity);
//    }

    /* 구매 가능 시간 확인 */
    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> isProductAvailable(@RequestParam Long productId) {
        boolean isAvailable = productService.isProductAvailable(productId);
        return ResponseEntity.ok(isAvailable);
    }

    /* 재고 수량 확인 & 재고 감소 */
    @PostMapping("/check-and-update-stock")
    public ResponseEntity<Boolean> checkAndUpdateStock(@RequestParam Long productId, @RequestParam int quantity) {
        boolean isStockUpdated = productService.checkAndUpdateStock(productId, quantity);
        return ResponseEntity.ok(isStockUpdated);
    }

    /* 주문 시 성패여부 분기처리 */
    @PostMapping("/update-stock")
    public ResponseEntity<Void> updateStock(@RequestParam Long productId,
                                            @RequestParam int quantity,
                                            @RequestParam boolean success) {
        productService.updateStock(productId, quantity, success);
        return ResponseEntity.ok().build();
    }

}