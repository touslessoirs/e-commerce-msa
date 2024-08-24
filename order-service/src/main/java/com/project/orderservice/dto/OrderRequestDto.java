package com.project.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {
    private List<OrderProductRequestDto> orderProducts;
    private ShippingRequestDto shipping;
    private boolean fromCart = false;    //장바구니 분기 처리
}




