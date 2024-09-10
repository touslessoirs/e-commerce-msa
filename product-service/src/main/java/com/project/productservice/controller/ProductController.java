package com.project.productservice.controller;

import com.project.productservice.dto.ProductIdsRequestDto;
import com.project.productservice.dto.ProductRequestDto;
import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public static final String AUTHORIZATION_KEY = "auth";
    public static final String ROLE_ADMIN = "ADMIN";

    private final ProductService productService;

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting;
    }

    /* 상품 등록 (관리자) */
    @PostMapping("/products")
    public ResponseEntity<ProductResponseDto> createProduct(@RequestHeader(AUTHORIZATION_KEY) String role,
                                                            @RequestBody ProductRequestDto productRequestDto) {
        if(!role.equals(ROLE_ADMIN)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ADMIN_ACCESS);
        }
        ProductResponseDto productResponseDto = productService.createProduct(productRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(productResponseDto);
    }

    /* 상품 수정 (관리자) */
    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductResponseDto> updateProduct(@RequestHeader(AUTHORIZATION_KEY) String role,
                                                            @PathVariable Long productId,
                                                            @RequestBody ProductRequestDto productRequestDto) {
        if(!role.equals(ROLE_ADMIN)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ADMIN_ACCESS);
        }
        ProductResponseDto productResponseDto = productService.updateProduct(productId, productRequestDto);
        return ResponseEntity.status(HttpStatus.OK).body(productResponseDto);
    }

    /* 상품 삭제 (관리자) */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity deleteProduct(@RequestHeader(AUTHORIZATION_KEY) String role, @PathVariable Long productId) {
        if(!role.equals(ROLE_ADMIN)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ADMIN_ACCESS);
        }
        productService.deleteProduct(productId);
        return ResponseEntity.status(HttpStatus.OK).body("상품이 삭제되었습니다.");
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

    /* 전체 상품 조회 */
    @GetMapping("/allProducts")
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponseDto> productResponseDtoPage = productService.getAllProducts(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(productResponseDtoPage);
    }

    /* 카테고리별로 상품 조회 */
    @GetMapping("/products")
    public Page<ProductResponseDto> getProductsByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getProductsByCategory(category, pageable);
    }
}