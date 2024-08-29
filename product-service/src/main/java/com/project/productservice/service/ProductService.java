package com.project.productservice.service;

import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
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

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
    public boolean checkProduct(Long productId, int quantity) {
        log.info("checkProduct 호출");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        LocalDateTime currentTime = LocalDateTime.now();
        return productRepository.isProductAvailable(productId, quantity, currentTime);
    }

    /**
     * 재고 update
     * stock += quantity
     *
     * @param productId
     * @param quantity (감소 -, 증가 +)
     */
    /* synchronized */
    /* Pessimistic Lock */
    @Transactional(readOnly = false)
    public synchronized void updateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setStock(product.getStock() + quantity);

        if (product.getStock() < 0) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        productRepository.save(product);
    }

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

}