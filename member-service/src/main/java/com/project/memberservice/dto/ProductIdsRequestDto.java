package com.project.memberservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductIdsRequestDto {
    private List<Long> productIds;
}
