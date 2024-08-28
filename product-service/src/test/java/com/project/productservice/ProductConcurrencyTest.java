package com.project.productservice;

import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import com.project.productservice.service.ProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class ProductConcurrencyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Long productId;
    private String productName;

    @BeforeEach
    public void setUp() {
        //테스트용 제품 생성
        Product product = new Product();
        product.setName("Test Product");
        product.setUnitPrice(1);
        product.setStock(100);         // 초기 재고 10
        product.setCategory("for test");
        product = productRepository.save(product);

        productId = product.getProductId();
        productName = product.getName();
    }

    /**
     * syncronized
     * 비관적 락
     *
     * @throws InterruptedException
     */
//    @Test
//    public void concurrentStockDecrease() throws InterruptedException {
//        int numberOfThreads = 100;
//        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
//        CountDownLatch latch = new CountDownLatch(numberOfThreads);
//
//        for (int i = 0; i < numberOfThreads; i++) {
//            executorService.execute(() -> {
//                try {
//                    productService.updateStock(productId, -1);
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await(10, TimeUnit.SECONDS); // 모든 스레드가 작업을 완료할 때까지 대기
//
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//        Assertions.assertEquals(0, product.getStock(), "최종 재고는 0이어야 합니다.");
//
//        executorService.shutdown();
//    }

    /**
     * 분산 락
     *
     * @throws InterruptedException
     */
    @Test
    public void concurrentStockDecreasebyDistributedLock() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    productService.updateStock(productId, -1);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS); // 모든 스레드가 작업을 완료할 때까지 대기

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        Assertions.assertEquals(0, product.getStock(), "최종 재고는 0이어야 합니다.");

        executorService.shutdown();
    }

}
