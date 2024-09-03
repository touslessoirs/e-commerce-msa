package com.project.productservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.productservice.entity.Product;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponseDto {
    private Long productId;
    private String name;
    private int unitPrice;
    private int stock;
    private String category;

    public ProductResponseDto(Product product) {
        this.productId = product.getProductId();
        this.name = product.getName();
        this.unitPrice = product.getUnitPrice();
        this.stock = product.getStock();
        this.category = product.getCategory();
    }
}
