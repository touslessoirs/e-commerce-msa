package com.project.memberservice.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductIdsRequestDto {
    private List<Long> productIds;
}
