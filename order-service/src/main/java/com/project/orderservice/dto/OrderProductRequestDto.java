package com.project.orderservice.dto;

import com.project.orderservice.entity.OrderProduct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderProductRequestDto {
    @NotNull
    private Long productId;
    @Min(1)
    private int quantity;
    @Min(1)
    private int unitPrice;

    public OrderProductRequestDto(OrderProduct orderProduct) {
        this.productId = orderProduct.getProductId();
        this.quantity = orderProduct.getQuantity();
        this.unitPrice = orderProduct.getUnitPrice();
    }
}
