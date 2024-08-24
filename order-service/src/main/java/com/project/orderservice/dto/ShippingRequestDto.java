package com.project.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingRequestDto {
    private String address;
    private String addressDetail;
    private String phone;
}
