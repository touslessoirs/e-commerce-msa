package com.project.productservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductIdsRequestDto {
    private List<Long> productIds;
}
