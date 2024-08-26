package com.project.productservice.service;

import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        //Product to ProductResponseDto
        ModelMapper modelMapper = new ModelMapper();
        return productList.stream()
                .map(order -> modelMapper.map(order, ProductResponseDto.class))
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
        ProductResponseDto productResponseDto = new ModelMapper().map(product, ProductResponseDto.class);
        return productResponseDto;
    }

    /**
     * 재고 update (for feign client)
     * stock -= orderquantity
     *
     * @param productId
     * @param orderQuantity
     */
    @Transactional
    public void updateStock(Long productId, int orderQuantity) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStock() < orderQuantity) {
            log.info("재고가 부족합니다.");
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        product.setStock(product.getStock() - orderQuantity);
        productRepository.save(product);
    }
}
