package com.project.orderservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "member-service")
public interface CartServiceClient {

    /* 장바구니 상품 삭제 (주문 완료 이후) */
    @DeleteMapping("/cart/fromOrder")
    public void deleteProductFromCart(@RequestParam String id, @RequestBody List<Long> cartProductIdList);
}
