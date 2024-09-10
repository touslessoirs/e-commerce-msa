package com.project.productservice.service;

import com.project.productservice.dto.ProductIdsRequestDto;
import com.project.productservice.dto.ProductRequestDto;
import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@EnableAsync
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 상품 등록
     * 
     * @param productRequestDto 등록하려는 상품 정보
     * @return 등록한 상품 정보
     */
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        Product product = new Product();
        product.setName(productRequestDto.getName());
        product.setUnitPrice(productRequestDto.getUnitPrice());
        product.setStock(productRequestDto.getStock());
        product.setCategory(productRequestDto.getCategory());

        productRepository.save(product);
        return new ProductResponseDto(product);
    }

    /**
     * 상품 수정
     * 
     * @param productId 수정하려는 상품의 ID
     * @param productRequestDto 수정하려는 상품 정보
     * @return 수정된 상품 정보
     */
    public ProductResponseDto updateProduct(Long productId, ProductRequestDto productRequestDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setName(productRequestDto.getName());
        product.setUnitPrice(productRequestDto.getUnitPrice());
        product.setStock(productRequestDto.getStock());
        product.setCategory(productRequestDto.getCategory());

        productRepository.save(product);
        return new ProductResponseDto(product);
    }

    /**
     * 상품 삭제
     *
     * @param productId 삭제하려는 상품의 ID
     */
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
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
     * 전체 상품 조회
     *
     * @return 전체 상품의 상세 정보 목록
     */
    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        return products.map(product -> new ProductResponseDto(product));
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

    public Page<ProductResponseDto> getProductsByCategory(String category, Pageable pageable) {
        Page<Product> products = productRepository.findByCategory(category, pageable);
        return products.map(product -> new ProductResponseDto(product));
    }
}