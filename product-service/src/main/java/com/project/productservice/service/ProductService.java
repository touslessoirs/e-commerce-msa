package com.project.productservice.service;

import com.project.productservice.dto.ProductResponseDto;
import com.project.productservice.entity.Product;
import com.project.productservice.exception.CustomException;
import com.project.productservice.exception.ErrorCode;
import com.project.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
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

        return productList.stream()
                .map(product -> new ProductResponseDto(
                        product.getName(),
                        product.getUnitPrice(),
                        product.getStock(),
                        product.getCategory()
                ))
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

        ProductResponseDto productResponseDto = new ProductResponseDto(
                product.getName(),
                product.getUnitPrice(),
                product.getStock(),
                product.getCategory()
        );
        return productResponseDto;
    }

    /**
     * 재고 update (for feign client)
     * stock -= orderquantity
     *
     * @param productId     (감소 -, 증가 +)
     * @param orderQuantity
     */
    @Transactional
    public void updateStock(Long productId, int orderQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setStock(product.getStock() + orderQuantity);

        if (product.getStock() < 0) {
            throw new CustomException(ErrorCode.OUT_OF_STOCK);
        }

        productRepository.save(product);
    }
}
