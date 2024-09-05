package com.project.productservice.service;

import com.project.productservice.dto.ProductIdsRequestDto;
import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@EnableAsync
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STOCK_KEY_PREFIX = "stock_ID: ";
    private static final String PURCHASE_KEY_PREFIX = "purchase_start_time_ID: ";

    /**
     * 전체 상품 조회
     *
     * @return 전체 상품의 상세 정보 목록
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
     * @param productId 상세 조회를 요청한 상품 ID
     * @return 해당 상품의 상세 정보
     */
    public ProductResponseDto getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductResponseDto productResponseDto = new ProductResponseDto(product);
        return productResponseDto;
    }

    /**
     * 여러 상품의 상세 조회
     * 
     * @param productIdsRequestDto 상세 조회를 요청한 상품 ID 목록
     * @return 해당 상품 목록의 상세 정보
     */
    public List<ProductResponseDto> getProductsDetails(ProductIdsRequestDto productIdsRequestDto) {
        List<Product> products = productRepository.findAllById(productIdsRequestDto.getProductIds());

        return products.stream()
                .map(ProductResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 특정 상품의 재고 수량 조회
     *
     * @param productId 조회하려는 상품의 ID
     * @return 해당 상품의 재고 수량
     */
    public int getProductStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return product.getStock();
    }

}