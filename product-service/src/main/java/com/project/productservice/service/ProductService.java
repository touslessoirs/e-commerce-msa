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
     * 재고 수량 & 구매 가능 시간 확인
     *
     * @param productId
     * @param quantity
     */
    public boolean isProductPurchasable(Long productId, int quantity) {
        log.info("isProductPurchasable 호출");

        boolean check1 = checkPurchaseStartTime(productId);
        boolean check2 = checkStock(productId, quantity);
        return check1 && check2;
    }

    /**
     * 구매 가능 시간 확인
     *
     * @param productId
     * @return
     */
    public boolean checkPurchaseStartTime(Long productId) {
        log.info("checkPurchaseStartTime 호출");

        String purchaseStartTimeKey = "purchaseStartTime_ID: " + productId;
        String purchaseStartTimeStr = redisTemplate.opsForValue().get(purchaseStartTimeKey);

        LocalDateTime purchaseStartTime;

        if (purchaseStartTimeStr == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            purchaseStartTime = product.getPurchaseStartTime();

            log.info("@@@@@@@@@@@@@@ PurchaseStartTime 캐싱");
            redisTemplate.opsForValue().set(purchaseStartTimeKey, purchaseStartTime.toString());
        } else {
            purchaseStartTime = LocalDateTime.parse(purchaseStartTimeStr);
        }

        if (LocalDateTime.now().isBefore(purchaseStartTime)) {
            throw new CustomException(ErrorCode.PURCHASE_TIME_INVALID);
        }

        return true;
    }

    /**
     * 재고 수량 확인
     *
     * @param productId
     * @param quantity
     */
    /* Redis */
    public boolean checkStock(Long productId, int quantity) {
        log.info("checkStock 호출");

        String stockKey = "stock_ID: " + productId;
        String stockStr = redisTemplate.opsForValue().get(stockKey);

        int stock;

        if (stockStr == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            stock = product.getStock();

            log.info("@@@@@@@@@@@@@@ Stock 캐싱");
            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        } else {
            stock = Integer.parseInt(stockStr);
        }

        if (stock < quantity) {
            throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
        }

        return true;
    }

    /**
     * 재고 update
     * stock += quantity
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
    /* Redis Cashing */
    @Transactional(readOnly = false)
    public synchronized void updateStock(Long productId, int quantity) {
        log.info("updateStock 호출");

        String stockKey = "stock_ID: " + productId;
        String stockStr = redisTemplate.opsForValue().get(stockKey);

        int stock;

        if (stockStr == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            stock = product.getStock();

            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        } else {
            stock = Integer.parseInt(stockStr);
        }

        int updatedStock = stock + quantity;

        // 재고가 부족한 경우
        if (updatedStock < 0) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        // Redis에서 재고 업데이트
        redisTemplate.opsForValue().set(stockKey, String.valueOf(updatedStock));

        // DB와 동기화
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setStock(updatedStock);
        productRepository.save(product);

        log.info("재고 수량이 업데이트되었습니다. Product ID: {}, Updated Stock: {}", productId, updatedStock);
    }
}