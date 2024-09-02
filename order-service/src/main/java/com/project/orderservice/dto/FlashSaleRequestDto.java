package com.project.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlashSaleRequestDto {
    @NotNull
    @Valid
    private OrderProductRequestDto orderProduct;
    @NotNull
    private ShippingRequestDto shipping;
}
