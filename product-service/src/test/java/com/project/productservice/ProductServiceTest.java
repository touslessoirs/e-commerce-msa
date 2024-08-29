package com.project.productservice;

import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.repository.ProductRepository;
import com.project.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

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
        boolean result = productService.isProductPurchasable(productId, 1);

        // then
        assertTrue(result, "Product should be available for purchase.");
    }

    /**
     * 구매 가능일 < 현재 시점
     */
//    @Test
    public void testCheckProductWhenNotAvailable1() {
        // given

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            productService.isProductPurchasable(productId, quantity);
        });

        assertEquals("아직 구매 가능한 시간이 아닙니다.", exception.getMessage());
    }

    /**
     * 재고 < 구매 요청 수량
     */
    @Test
    public void testCheckProductWhenNotAvailable2() {
        // given
        int requestedQuantity = quantity + 1;

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            productService.isProductPurchasable(productId, requestedQuantity);
        });

        assertEquals("재고가 부족하여 결제할 수 없습니다.", exception.getMessage());
    }
}
