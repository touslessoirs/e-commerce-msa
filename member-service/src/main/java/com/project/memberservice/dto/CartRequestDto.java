package com.project.memberservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartRequestDto {
    @NotNull
    @Valid
    private List<CartProductRequestDto> cartProducts;
}
