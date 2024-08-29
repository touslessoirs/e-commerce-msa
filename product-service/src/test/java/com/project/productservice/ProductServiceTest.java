package com.project.productservice;

import com.project.productservice.entity.Product;
import com.project.productservice.repository.ProductRepository;
import com.project.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Long productId;
    private int quantity;
    private String productName;
    LocalDateTime currentTime = LocalDateTime.now();

    @BeforeEach
    public void setUp() {
        //테스트용 제품 생성
        Product product = new Product();
        product.setName("Test Product");
        product.setUnitPrice(1);
        product.setStock(100);         // 초기 재고
        product.setCategory("for test");
        product.setPurchaseStartTime(currentTime.minusDays(1));  // 현재 시간 1일 전
//        product.setPurchaseStartTime(currentTime.plusDays(1));  // 현재 시간 1일 후
        product = productRepository.save(product);

        productId = product.getProductId();
        productName = product.getName();
        quantity = product.getStock();
    }

    @Test
    public void testCheckProductWhenAvailable() {
        // given

        // when
        boolean result = productService.checkProduct(productId, 1);

        // then
        assertTrue(result, "Product should be available for purchase.");
    }

    /**
     * 구매 가능일 < 현재 시점
     */
    @Test
    public void testCheckProductWhenNotAvailable1() {
        // given

        // when
        boolean result = productService.checkProduct(productId, quantity);

        // then
        assertFalse(result, "Product should not be available for purchase.");
    }

    /**
     * 재고 < 구매 요청 수량
     */
    @Test
    public void testCheckProductWhenNotAvailable2() {
        // given

        // when
        boolean result = productService.checkProduct(productId, quantity+1);

        // then
        assertFalse(result, "Product should not be available for purchase.");
    }
}
