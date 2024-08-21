package com.project.productservice.controller;

import com.project.productservice.entity.Product;
import com.project.productservice.service.ProductService;
import com.project.productservice.vo.Greeting;
import com.project.productservice.vo.ProductResponse;
import jakarta.persistence.criteria.Order;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/product-service")
public class ProductController {

    private final Environment env;
    private final ProductService productService;

    public ProductController(Environment env, ProductService memberService) {
        this.env = env;
        this.productService = memberService;
    }

    @Autowired
    private Greeting greeting;

    @GetMapping("/health-check")
    public String status() {
        return String.format("PRODUCT SERVICE Running on PORT %s", env.getProperty("server.port"));

    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request, HttpServletResponse response) {
        return greeting.getMessage();
    }

    /* 전체 상품 조회 */
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        Iterable<Product> productList = productService.getAllProducts();

        List<ProductResponse> result = new ArrayList<>();
        productList.forEach(v -> {
            result.add(new ModelMapper().map(v, ProductResponse.class));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /* 사용자별 상품 주문 */
    @PostMapping("/{memberId}/orders")
    public ResponseEntity<ProductResponse> addOrder(@PathVariable("memberId") String memberId, @RequestBody Order order) {

        return null;
    }


    /* 사용자별 주문 내역 조회 */
    @GetMapping("/{memberId}/orders")
    public ResponseEntity<Order> getOrder(@PathVariable("memberId") String memberId) {

        return null;
    }
}
