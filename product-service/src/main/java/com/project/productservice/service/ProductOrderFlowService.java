package com.project.productservice.service;

import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@EnableAsync
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductOrderFlowService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STOCK_KEY_PREFIX = "stock_ID: ";
    private static final String PURCHASE_KEY_PREFIX = "purchase_start_time_ID: ";

    /**
     * 주문 가능 여부 확인 (주문 요청 시)
     *
     * 1. 현재 구매 가능 시점인지 확인
     * 2. 요청한 수량이 주문 가능한 재고량을 초과하지 않는지 확인
     *
     * @param productId 확인을 요청한 상품의 ID
     * @param quantity 확인을 요청한 상품 수량
     * @return 주문 가능 하면 true, 주문 불가능하면 false
     */
    @Transactional
    public boolean checkProductForOrder(Long productId, int quantity) {
        // 1. 구매 가능 시간 확인
        if (checkPurchaseTime(productId)) {
            //2. 주문 가능한 재고량인지 확인
            return checkStock(productId, quantity);
        }

        return false;
    }

    /**
     * 구매 가능 시간 확인
     *
     * @param productId 확인을 요청한 상품의 ID
     * @return 현재 구매 가능 시점이면 true, 그렇지 않으면 false
     */
    public boolean checkPurchaseTime(Long productId) {
        String purchaseStartTimeKey = PURCHASE_KEY_PREFIX + productId;
        String purchaseStartTimeStr = redisTemplate.opsForValue().get(purchaseStartTimeKey);

        LocalDateTime purchaseStartTime;

        if (purchaseStartTimeStr == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            purchaseStartTime = product.getPurchaseStartTime();

            redisTemplate.opsForValue().set(purchaseStartTimeKey, purchaseStartTime.toString());
        } else {
            purchaseStartTime = LocalDateTime.parse(purchaseStartTimeStr);
        }

        if (LocalDateTime.now().isAfter(purchaseStartTime)) {
            return true;
        }

        throw new CustomException(ErrorCode.PURCHASE_TIME_INVALID);
    }

    /**
     * 주문 가능한 재고량인지 확인
     *
     * @param productId 확인을 요청한 상품의 ID
     * @param quantity 확인을 요청한 상품 수량
     * @return 해당 상품의 재고 수량이 충분하면 true, 그렇지 않으면 false
     */
    public boolean checkStock(Long productId, int quantity) {
        // 1. 캐시 조회
        String stockKey = STOCK_KEY_PREFIX + productId;
        String stockValue = redisTemplate.opsForValue().get(stockKey);

        int stock;

        if (stockValue != null) {
            stock = Integer.parseInt(stockValue);
        } else {
            // 2. 캐시에 재고 정보가 없는 경우 -> 캐싱
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            stock = product.getStock();
            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        }

        // 3. 재고 확인
        if (stock >= quantity) {
            return true;
        }

        log.error("STOCK_INSUFFICIENT");
        throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
    }

    /**
     * 상품의 재고 수량 감소
     *
     * Redis 캐시에 저장된 재고를 우선적으로 조회하고, 캐시에 값이 없을 경우 데이터베이스에서 조회힌 후 캐싱한다.
     * Redis 캐시에 저장된 재고를 우선적으로 감소시키고, 이후 비동기적으로 데이터베이스의 재고를 업데이트한다.
     *
     * @param productId 감소시킬 상품의 ID
     * @param quantity 감소시킬 재고 수량
     * @throws CustomException 재고가 부족할 경우 STOCK_INSUFFICIENT 예외 발생
     */
    @Transactional
    public void reduceStock(Long productId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        // 1. REDIS 재고 감소
        String stockValue = redisTemplate.opsForValue().get(stockKey);
        int stock;

        if (stockValue != null) {
            stock = Integer.parseInt(stockValue);
            if (stock < quantity) {
                throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
            }
            redisTemplate.opsForValue().decrement(stockKey, quantity);
            log.info("REDIS STOCK REDUCE - KEY: {}, VALUE: {}", stockKey, stock-quantity);
        } else {
            // 캐시에 값이 없을 경우 -> 데이터베이스에서 재고 조회 후 캐싱
            stock = productService.getProductStock(productId) - quantity;
            if (stock < 0) {
                throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
            }
            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
            log.info("REDIS STOCK SET - KEY: {}, VALUE: {}", stockKey, stock);
        }

        // 2. DB 재고 감소 (비동기)
        reduceDatabaseStockAsync(productId, quantity);
    }

    /* DB 재고 감소 (비동기) */
    @Async
    public void reduceDatabaseStockAsync(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        int updateStock = product.getStock() - quantity;
        if (updateStock < 0) {
            throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
        }

        product.setStock(updateStock);
        productRepository.save(product);
        log.info("[ASYNC] DB STOCK REDUCE - ID: {}, VALUE: {}", product.getProductId(), product.getStock());
    }

    /**
     * 상품의 재고 수량 증가
     *
     * 주문 실패, 결제 실패, 주문 취소 또는 반품 승인 시 해당 상품의 재고를 롤백한다.
     * Redis 캐시에 저장된 재고를 우선적으로 조회하고, 캐시에 값이 없을 경우 데이터베이스에서 조회힌 후 캐싱한다.
     * Redis 캐시에 저장된 재고를 우선적으로 롤백(증가)시키고, 이후 비동기적으로 데이터베이스의 재고를 업데이트한다.
     *
     * @param productId 롤백(증가)시킬 상품의 ID
     * @param quantity 롤백(증가)시킬 상품의 수량
     */
    @Transactional
    public void rollbackStock(Long productId, int quantity) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        // 1. REDIS 재고 롤백
        String stockValue = redisTemplate.opsForValue().get(stockKey);
        int stock;

        if (stockValue != null) {
            stock = Integer.parseInt(stockValue);
            redisTemplate.opsForValue().increment(stockKey, quantity);
            log.info("REDIS STOCK ROLLBACK - KEY: {}, VALUE: {}", productId, stock+quantity);
        } else {
            // 캐시에 값이 없을 경우 -> 데이터베이스에서 재고 조회 후 캐싱
            int rollbackStock = productService.getProductStock(productId) + quantity;
            redisTemplate.opsForValue().set(stockKey, String.valueOf(rollbackStock));
            log.info("REDIS STOCK SET - KEY: {}, VALUE: {}", stockKey, rollbackStock);
        }

        // 2. DB 재고 롤백 (비동기)
        rollbackDatabaseStockAsync(productId, quantity);
    }

    /* DB 재고 롤백 (비동기) */
    @Async
    public void rollbackDatabaseStockAsync(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
        log.info("[ASYNC] DB STOCK ROLLBACK - ID: {}, VALUE: {}", product.getProductId(), product.getStock());
    }
}
