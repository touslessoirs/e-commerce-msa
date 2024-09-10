package com.project.productservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequestDto {
    @NotNull
    private String name;
    @NotNull
    private int unitPrice;
    @NotNull
    private int stock;
    @NotNull
    private String category;
}
