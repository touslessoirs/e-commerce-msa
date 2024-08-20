package com.project.productservice.dto;

import jakarta.persistence.*;
import lombok.Data;

@Data
public class ProductDto {
    private Long productId;
    private String name;
    private int unitPrice;
    private int stock;
    private String category;

    private Long orderId;
    private Long memberId;
}
