package com.project.productservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductIdsRequestDto {
    private List<Long> productIds;
}
