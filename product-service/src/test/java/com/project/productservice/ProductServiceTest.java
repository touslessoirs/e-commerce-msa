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
    private LocalDateTime currentTime = LocalDateTime.now();

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
    }

//    @Test
//    public void testCheckProductWhenAvailable() {
//        // given
//
//        // when
//
//        // then
//
//    }

    /**
     * 구매 가능일 < 현재 시점
     */
//    @Test
//    public void testCheckProductWhenNotAvailable1() {
//        // given
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            productService.checkAndUpdateStock(productId, quantity);
//        });
//
//        assertEquals("아직 구매 가능한 시간이 아닙니다.", exception.getMessage());
//    }

    /**
     * 재고 < 구매 요청 수량
     */
//    @Test
//    public void testCheckProductWhenNotAvailable2() {
//        // given
//        int requestedQuantity = quantity + 1;
//
//        // when & then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            productService.checkAndUpdateStock(productId, requestedQuantity);
//        });
//
//        assertEquals("재고가 부족하여 결제할 수 없습니다.", exception.getMessage());
//    }

    @Test
    public void testCheckPurchaseStartTime_whenTimeIsValid() {
        // given
        // 현재 시간이 제품의 구매 시작 시간 이후일 때

        // when
        boolean result = productService.checkPurchaseStartTime(productId);

        // then
        assertTrue(result);
    }

    @Test
    public void testCheckPurchaseStartTime_whenTimeIsInvalid() {
        // given
        Product product = new Product();
        product.setName("Future Product");
        product.setUnitPrice(1);
        product.setStock(100);
        product.setCategory("for test");
        product.setPurchaseStartTime(currentTime.plusDays(1));  // 현재 시간 1일 후
        product = productRepository.save(product);

        Long futureProductId = product.getProductId();

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            productService.checkPurchaseStartTime(futureProductId);
        });

        assertEquals("아직 구매 가능한 시간이 아닙니다.", exception.getMessage());
    }


}
