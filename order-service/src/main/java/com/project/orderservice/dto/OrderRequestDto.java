package com.project.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {
    @NotNull
    @Valid
    private List<OrderProductRequestDto> orderProducts;
    @NotNull
    private ShippingRequestDto shipping;
    @NotNull
    private boolean fromCart = false;    //장바구니 분기 처리
}
