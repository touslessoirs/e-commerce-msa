package com.project.memberservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartUpdateRequestDto {
    @NotNull
    private List<CartProductRequestDto> cartProducts;
}
