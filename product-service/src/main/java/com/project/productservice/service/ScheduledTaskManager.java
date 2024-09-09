package com.project.productservice.service;

import com.project.productservice.entity.Product;
import com.project.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@EnableKafka
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProductRepository productRepository;

    private static final String STOCK_KEY_PREFIX = "stock_ID: ";
    private static final String PURCHASE_KEY_PREFIX = "purchase_start_time_ID: ";

    /**
     * Redis-DB 재고 및 구매 가능 시간 동기화
     */
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정 실행
    public void synchronizeDbAndRedis() {
        List<Product> allProducts = productRepository.findAll();  // DB에서 모든 상품 조회

        for (Product product : allProducts) {
            // 1. 재고 동기화
            String stockKey = STOCK_KEY_PREFIX + product.getProductId();
            String stockValue = redisTemplate.opsForValue().get(stockKey);
            int dbStock = product.getStock();

            if (stockValue != null) {
                int redisStock = Integer.parseInt(stockValue);

                // Redis 재고와 DB 재고 불일치 -> DB를 기준으로 동기화
                if (redisStock != dbStock) {
                    log.info("재고 불일치 - ID {}: Redis = {}, DB = {}", product.getProductId(), redisStock, dbStock);
                    redisTemplate.opsForValue().set(stockKey, String.valueOf(dbStock));
                }
            }

            // 2. 구매 가능 시간 동기화
            String purchaseStartTimeKey = PURCHASE_KEY_PREFIX + product.getProductId();
            String purchaseStartTimeValue = redisTemplate.opsForValue().get(purchaseStartTimeKey);
            LocalDateTime dbPurchaseStartTime = product.getPurchaseStartTime();

            if (purchaseStartTimeValue != null) {
                LocalDateTime redisPurchaseStartTime = LocalDateTime.parse(purchaseStartTimeValue);

                // Redis 구매 시간과 DB 구매 시간 불일치 -> DB를 기준으로 동기화
                if (!redisPurchaseStartTime.equals(dbPurchaseStartTime)) {
                    log.info("구매 시간 불일치 - ID {}: Redis = {}, DB = {}", product.getProductId(), redisPurchaseStartTime, dbPurchaseStartTime);
                    redisTemplate.opsForValue().set(purchaseStartTimeKey, dbPurchaseStartTime.toString());
                }
            }
        }

        log.info("Redis-DB 동기화 작업이 완료되었습니다.");
    }

}
