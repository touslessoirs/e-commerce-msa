package com.project.memberservice.controller;

import com.project.memberservice.dto.CartRequestDto;
import com.project.memberservice.dto.ProductResponseDto;
import com.project.memberservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    /* 장바구니 담기 */
    @PostMapping("")
    public ResponseEntity addCart(@RequestHeader("X-Member-Id") String id,
                                  @Valid @RequestBody CartRequestDto cartRequestDto) {
        cartService.addCart(id, cartRequestDto);
        return ResponseEntity.ok("장바구니에 추가되었습니다.");
    }

    /* 장바구니 (수량)수정 */
    @PutMapping("")
    public ResponseEntity updateQuantity(@RequestHeader("X-Member-Id") String id,
                                         @Valid @RequestBody CartRequestDto cartRequestDto,
                                         @RequestParam Boolean isIncrease) {
        cartService.updateQuantity(id, cartRequestDto, isIncrease);
        return ResponseEntity.ok("장바구니가 수정되었습니다.");
    }

    /* 장바구니 삭제 */
    @DeleteMapping("")
    public ResponseEntity deleteProduct(@RequestHeader("X-Member-Id") String id,
                                        @RequestBody List<Long> cartProductIdList) {
        cartService.deleteProduct(id, cartProductIdList);
        return ResponseEntity.ok("장바구니에서 해당 상품이 삭제되었습니다.");
    }

    /* 장바구니 삭제 (주문 완료 이후) */
    @DeleteMapping("/fromOrder")
    public void deleteProductFromCart(@RequestParam String id, @RequestBody List<Long> cartProductIdList) {
        log.info("id: {}, productIds size: {}", id, cartProductIdList.size());

        cartService.deleteProduct(id, cartProductIdList);
    }

    /* 장바구니 조회 */
    @GetMapping("")
    public List<ProductResponseDto> getCart(@RequestHeader("X-Member-Id") String id) {
        return cartService.getCart(id);
    }

}
