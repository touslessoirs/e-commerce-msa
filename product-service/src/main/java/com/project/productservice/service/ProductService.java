package com.project.productservice.service;

import com.project.productservice.aop.DistributedLock;
import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
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
     * 재고 수량 & 구매 가능 시간 확인
     *
     * @param productId
     * @param quantity
     */
//    public boolean isProductPurchasable(Long productId, int quantity) {
//        log.info("isProductPurchasable 호출");
//
//        boolean check1 = checkPurchaseStartTime(productId);
//        boolean check2 = checkStock(productId, quantity);
//        return check1 && check2;
//    }

    /**
     * 재고 수량 확인
     *
     * @param productId
     * @param quantity
     */
    /* Redis */
//    public boolean checkStock(Long productId, int quantity) {
//        log.info("checkStock 호출");
//
//        String stockKey = "stock_ID: " + productId;
//        String stockStr = redisTemplate.opsForValue().get(stockKey);
//
//        int stock;
//
//        if (stockStr == null) {
//            Product product = productRepository.findById(productId)
//                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//            stock = product.getStock();
//
//            log.info("@@@@@@@@@@@@@@ Stock 캐싱");
//            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
//        } else {
//            stock = Integer.parseInt(stockStr);
//        }
//
//        if (stock < quantity) {
//            throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
//        }
//
//        return true;
//    }

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

    /**
     * 구매 가능 시간 확인 - Redis caching
     *
     * @param productId
     * @return
     */
//    public boolean checkPurchaseStartTime(Long productId) {
//        log.info("checkPurchaseStartTime 호출");
//
//        String purchaseStartTimeKey = "purchaseStartTime_ID: " + productId;
//        String purchaseStartTimeStr = redisTemplate.opsForValue().get(purchaseStartTimeKey);
//
//        LocalDateTime purchaseStartTime;
//
//        if (purchaseStartTimeStr == null) {
//            Product product = productRepository.findById(productId)
//                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//            purchaseStartTime = product.getPurchaseStartTime();
//
//            log.info("@@@@@@@@@@@@@@ PurchaseStartTime 캐싱");
//            redisTemplate.opsForValue().set(purchaseStartTimeKey, purchaseStartTime.toString());
//        } else {
//            purchaseStartTime = LocalDateTime.parse(purchaseStartTimeStr);
//        }
//
//        if (LocalDateTime.now().isAfter(purchaseStartTime)) {
//            return true;
//        }
//
//        throw new CustomException(ErrorCode.PURCHASE_TIME_INVALID);
//    }

//    @DistributedLock(key = "'PRODUCTID-' + #productId")
//    public void rollbackStock(Long productId, int quantity) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//
//        product.setStock(product.getStock() + quantity);
//        productRepository.save(product);
//    }

    /**
     * 구매 가능 시간 확인
     * 
     * @param productId
     * @return
     */
    public boolean isProductAvailable(Long productId) {
        log.info("isProductAvailable 호출");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(product.getPurchaseStartTime());  // 구매 가능 시간 확인
    }

    /**
     * 재고 확인 & Redis 재고 감소
     * 
     * @param productId
     * @param quantity
     * @return
     * @throws CustomException
     */
//    @DistributedLock(key = "'PRODUCTID-' + #productId")
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public boolean checkAndUpdateStock(Long productId, int quantity) {
//        String stockKey = STOCK_KEY_PREFIX + productId;
//        String stockValue = redisTemplate.opsForValue().get(stockKey);
//
//        int stock;
//
//        if (stockValue == null) {
//            Product product = productRepository.findById(productId)
//                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//            stock = product.getStock();
//
//            log.info("@@@@@@@@@@@@@@ STOCK 캐싱");
//            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
//        } else {
//            stock = Integer.parseInt(stockValue);
//        }
//
//        if (stock < quantity) {
//            log.info("STOCK_INSUFFICIENT");
//            return false;
////            throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
//        } else {
//            // 재고 감소 처리
//            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock - quantity));
//            log.info("REDIS STOCK UPDATE: {}", redisTemplate.opsForValue().get(stockKey));
//            return true;
//        }
//    }
    @DistributedLock(key = "'PRODUCTID-' + #productId")
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean checkAndUpdateStock(Long productId, int quantity) {
        log.info("checkAndUpdateStock 호출");
        String stockKey = STOCK_KEY_PREFIX + productId;

        // Redis에서 재고 정보를 가져옵니다.
        String stockValue = redisTemplate.opsForValue().get(stockKey);
        int stock;

        // Redis에 재고 정보가 없는 경우, 데이터베이스에서 재고를 조회하여 Redis에 캐싱합니다.
        if (stockValue == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            stock = product.getStock();

            log.info("STOCK 캐싱: {}", stock);
            redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        } else {
            stock = Integer.parseInt(stockValue);
        }

        // Lua 스크립트를 사용하여 원자적으로 재고 감소 처리
        String luaScript = "if (redis.call('exists', KEYS[1]) == 1) then "
                + "local stock = tonumber(redis.call('get', KEYS[1])); "
                + "if (stock >= tonumber(ARGV[1])) then "
                + "redis.call('decrby', KEYS[1], ARGV[1]); "
                + "return 1; "
                + "end; "
                + "return 0; "
                + "end; "
                + "return 0;";

        // Redis에 Lua 스크립트를 실행합니다.
        Long result = redisTemplate.execute(
                (RedisCallback<Long>) connection -> connection.eval(
                        luaScript.getBytes(),
                        ReturnType.INTEGER,
                        1,
                        stockKey.getBytes(),
                        String.valueOf(quantity).getBytes()
                )
        );

        // Lua 스크립트 실행 결과에 따라 재고가 충분하지 않을 경우 false를 반환합니다.
        if (result == 0) {
            log.info("STOCK_INSUFFICIENT: 재고 부족으로 주문을 처리할 수 없습니다.");
            return false;
        }

        log.info("REDIS STOCK UPDATE: {} (productId: {})", redisTemplate.opsForValue().get(stockKey), productId);

        // 데이터베이스 작업만 트랜잭션으로 처리
        return updateDatabaseStock(productId, quantity);
    }

    @Transactional
    public boolean updateDatabaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStock() < quantity) {
            throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        log.info("DB STOCK UPDATE: {} (productId: {})", product.getStock(), productId);
        return true;
    }

    /**
     * 결제 완료 시 호출됨
     * 결제 성공 -> DB 재고 감소 (Redis와 동기화)
     * 결제 실패 -> Redis 재고 롤백
     *
     * @param productId
     * @param quantity
     * @param success
     */
    @Transactional
    public void updateStock(Long productId, int quantity, boolean success) {
//        if (success) {
//            // 결제 성공 시 DB에 재고 반영
//            Product product = productRepository.findById(productId)
//                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
//
//            // 재고가 음수가 되지 않도록 보장
//            if (product.getStock() < quantity) {
//                throw new CustomException(ErrorCode.STOCK_INSUFFICIENT);
//            }
//
//            product.setStock(product.getStock() - quantity);
//            productRepository.save(product);
//            log.info("DB STOCK UPDATE: {}", product.getStock());
//        } else {
//            // 결제 실패 시 Redis 재고 롤백
//            redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + productId, quantity);
//            log.info("REDIS STOCK ROLLBACK: {}", redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + productId));
//        }

        if(!success){
            // 1. DB에서 재고 복구
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            product.setStock(product.getStock() + quantity);
            productRepository.save(product);
            log.info("DB STOCK ROLLBACK: {}", product.getStock());

            // 2. Redis에서도 재고 롤백
            redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + productId, quantity);
            log.info("REDIS STOCK ROLLBACK: {}", redisTemplate.opsForValue().get(STOCK_KEY_PREFIX + productId));
        }
    }

}