package com.project.productservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponseDto {
    private Long productId;
    private String name;
    private int unitPrice;
    private int stock;
    private String category;

    public ProductResponseDto(String name, int unitPrice, int stock, String category) {
        this.name = name;
        this.unitPrice = unitPrice;
        this.stock = stock;
        this.category = category;
    }
}
