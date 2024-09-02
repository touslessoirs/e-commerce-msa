package com.project.productservice.service;

import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@EnableAsync
@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedissonClient redissonClient;

    private static final String STOCK_KEY_PREFIX = "stock_ID: ";
    private static final String PURCHASE_KEY_PREFIX = "purchase_start_time_ID: ";

    public ProductService(ProductRepository productRepository,
                          RedisTemplate redisTemplate,
                          RedissonClient redissonClient) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
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
     * 주문 요청 - 주문 가능 여부 확인
     *
     * @param productId
     * @param quantity
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
     * @param productId
     * @return 구매 가능 여부
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

        log.error("PURCHASE_TIME_INVALID");
        throw new CustomException(ErrorCode.PURCHASE_TIME_INVALID);
    }

    /**
     * 주문 가능한 재고량인지 확인
     *
     * @param productId
     * @param quantity
     * @return 재고 수량 충분한지 여부
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
     * 재고 수량 감소
     *
     * @param productId
     * @param quantity
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
            stock = getProductStock(productId) - quantity;
            if (stock < 0) {
                throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
            }
            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
            log.info("REDIS STOCK SET - KEY: {}, VALUE: {}", stockKey, stock);
        }

        // 2. DB 재고 감소 (비동기)
        reduceDatabaseStockAsync(productId, quantity);
    }

    @Async
    public void reduceDatabaseStockAsync(Long productId, int quantity) {
        // DB 재고 감소 (비동기)
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
     * 재고 수량 증가(결제 실패 시 rollback)
     *
     * @param productId
     * @param quantity
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
            int rollbackStock = getProductStock(productId) + quantity;
            redisTemplate.opsForValue().set(stockKey, String.valueOf(rollbackStock));
            log.info("REDIS STOCK SET - KEY: {}, VALUE: {}", stockKey, rollbackStock);
        }

        // 2. DB 재고 롤백 (비동기)
        rollbackDatabaseStockAsync(productId, quantity);
    }

    @Async
    public void rollbackDatabaseStockAsync(Long productId, int quantity) {
        // DB 재고 롤백 (비동기)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
        log.info("[ASYNC] DB STOCK ROLLBACK - ID: {}, VALUE: {}", product.getProductId(), product.getStock());
    }

    /**
     * 특정 상품의 재고 조회
     *
     * @param productId
     * @return stock
     */
    public int getProductStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return product.getStock();
    }
}