package com.project.productservice.service;

import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STOCK_KEY_PREFIX = "product:stock:";

    public ProductService(ProductRepository productRepository,
                          RedisTemplate redisTemplate) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 전체 상품 조회
     *
     * @return Product List
     */
    public List<ProductResponseDto> getAllProducts() {
        Iterable<Product> products = productRepository.findAll();

        //Iterable to List
        List<Product> productList = StreamSupport.stream(products.spliterator(), false)
                .collect(Collectors.toList());

        return productList.stream()
                .map(product -> new ProductResponseDto(product))
                .collect(Collectors.toList());
    }

    /**
     * 상품 상세 조회
     *
     * @param productId
     * @return ProductResponseDto
     */
    public ProductResponseDto getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductResponseDto productResponseDto = new ProductResponseDto(product);
        return productResponseDto;
    }

    /**
     * 주문 시 재고 관련 처리
     * 1. 구매 가능 시간 확인
     * 2. 재고 확인 및 수량 감소
     *
     * @param productId
     * @param quantity  (감소 -, 증가 +)
     */

    /* synchronized */
    /* Pessimistic Lock */
//    @Transactional(readOnly = false)
//    public synchronized void updateStock(Long productId, int quantity) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//
//        product.setStock(product.getStock() + quantity);
//
//        if (product.getStock() < 0) {
//            throw new CustomException(ErrorCode.OUT_OF_STOCK);
//        }
//
//        productRepository.save(product);
//    }

    /* Optimistic Lock */
//    @Transactional
//    public synchronized void updateStock(Long productId, int quantity) {
//
//        int retryCount = 3; // 재시도 횟수 설정
//        while (retryCount > 0) {
//            try {
//                Product product = productRepository.findById(productId)
//                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//
//                product.setStock(product.getStock() + quantity);
//
//                if (product.getStock() < 0) {
//                    throw new CustomException(ErrorCode.OUT_OF_STOCK);
//                }
//
//                productRepository.save(product);
//                break;
//            } catch (OptimisticLockException e) {
//                log.info("낙관적 락 충돌 - 재시도");
//
//                retryCount--;
//                if (retryCount == 0) {
//                    throw new CustomException(ErrorCode.CONCURRENCY_FAILURE);
//                }
//            }
//        }
//    }

    /* Distributed Lock */
//    @DistributedLock(key = "'PRODUCTID-' + #productId")
//    public void updateStock(Long productId, int quantity) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//
//        product.setStock(product.getStock() + quantity);
//
//        if (product.getStock() < 0) {
//            throw new CustomException(ErrorCode.OUT_OF_STOCK);
//        }
//
//        productRepository.save(product);
//    }

//    /* Redis Caching */
//    @DistributedLock(key = "'PRODUCTID-' + #productId")
//    public void checkAndUpdateStock(Long productId, int quantity) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//
    // 구매 가능 시간 확인
//        if (checkPurchaseStartTime(productId)) {
//
//        // 재고 수량 확인
//        String stockKey = "stock_ID: " + productId;
//        String stockStr = redisTemplate.opsForValue().get(stockKey);
//
//        int stock;
//
//        if (stockStr == null) {
//            stock = product.getStock();
//            log.info("@@@@@@@@@@@@@@ STOCK 캐싱");
//            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
//        } else {
//            stock = Integer.parseInt(stockStr);
//        }
//
//        if (stock < quantity) {
//            throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
//        }
//
//        int updatedStock = stock - quantity;
//        if (updatedStock < 0) {
//            throw new CustomException(ErrorCode.OUT_OF_STOCK);
//        }
//
//        // DB 업데이트
//        product.setStock(updatedStock);
//        productRepository.save(product);
//
//        // Redis 업데이트
//        redisTemplate.opsForValue().set(stockKey, String.valueOf(updatedStock));
//
//        log.info("재고 수량이 업데이트되었습니다. Product ID: {}, Updated Stock: {}", productId, updatedStock);
//    }

    /* Caching + Pessimistic Lock */
    @Transactional(readOnly = false)
    public boolean processPurchase(Long productId, int quantity) {
        log.info("processPurchase 호출");

        // 1. 구매 가능 시간 확인
        validatePurchaseTime(productId);

        // 2. 재고 확인 및 수량 감소
        return checkAndUpdateStock(productId, quantity);
    }

    /**
     * 구매 가능 시간 확인
     *
     * @param productId
     * @return
     */
    public boolean validatePurchaseTime(Long productId) {
        log.info("validatePurchaseTime 호출");

        String purchaseStartTimeKey = "purchaseStartTime_ID: " + productId;
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
     * 재고 확인 및 수량 감소 (Redis, DB)
     *
     * @param productId
     * @param quantity
     * @return
     */
    @Transactional
    public boolean checkAndUpdateStock(Long productId, int quantity) {
        log.info("checkAndUpdateStock 호출");

        // 1. Pessimistic Lock
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 캐싱된 정보 조회
        String stockKey = STOCK_KEY_PREFIX + productId;
        String stockValue = redisTemplate.opsForValue().get(stockKey);

        int stock;

        if (stockValue != null) {
            stock = Integer.parseInt(stockValue);
        } else {
            // 2-1. 캐시에 재고 정보가 없는 경우 -> 캐싱
            stock = product.getStock();
            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        }

        // 3. 재고 확인
        if (stock < quantity) {
            throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
        }

        int updatedStock = stock - quantity;

        // 4-1. DB 재고 감소
        product.setStock(updatedStock);
        productRepository.save(product);
        log.info("DB STOCK UPDATE: {} (productId: {})", updatedStock, productId);

        // 4-2. Redis 재고 감소
        redisTemplate.opsForValue().set(stockKey, String.valueOf(updatedStock));
        log.info("REDIS STOCK UPDATE: {} (productId: {})", updatedStock, productId);

        return true;
    }

    /**
     * 결제 실패 -> 재고 rollback (Redis, DB)
     *
     * @param productId
     * @param quantity
     */
    @Transactional
    public void rollbackStock(Long productId, int quantity) {
        // 1. DB 재고 롤백
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
        log.info("DB STOCK ROLLBACK: {} (productId : {})", product.getStock(), productId);

        // 2. Redis 재고 롤백
        redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + productId, quantity);
        log.info("REDIS STOCK ROLLBACK: {} (productId : {})", redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + productId), productId);
    }

}