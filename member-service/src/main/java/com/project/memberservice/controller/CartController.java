package com.project.memberservice.controller;

import com.project.memberservice.dto.CartRequestDto;
import com.project.memberservice.dto.ProductResponseDto;
import com.project.memberservice.security.UserDetailsImpl;
import com.project.memberservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity addCart(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                  @Valid @RequestBody CartRequestDto cartRequestDto) {
        cartService.addCart(userDetails, cartRequestDto);
        return ResponseEntity.ok("장바구니에 추가되었습니다.");
    }

    /* 장바구니 (수량)수정 */
    @PutMapping("")
    public ResponseEntity updateQuantity(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                         @Valid @RequestBody CartRequestDto cartRequestDto,
                                         @RequestParam Boolean isIncrease) {
        cartService.updateQuantity(userDetails, cartRequestDto, isIncrease);
        return ResponseEntity.ok("장바구니가 수정되었습니다.");
    }

    /* 장바구니 삭제 */
    @DeleteMapping("")
    public ResponseEntity deleteProduct(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                        @RequestBody List<Long> cartProductIdList) {
        cartService.deleteProduct(userDetails, cartProductIdList);
        return ResponseEntity.ok("장바구니에서 해당 상품이 삭제되었습니다.");
    }

    /* 장바구니 조회 */
    @GetMapping("")
    public List<ProductResponseDto> getCart(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return cartService.getCart(userDetails);
    }
}
