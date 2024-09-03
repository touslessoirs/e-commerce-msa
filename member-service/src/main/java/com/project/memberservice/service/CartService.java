package com.project.memberservice.service;

import com.project.memberservice.client.ProductServiceClient;
import com.project.memberservice.dto.CartProductRequestDto;
import com.project.memberservice.dto.CartRequestDto;
import com.project.memberservice.dto.ProductIdsRequestDto;
import com.project.memberservice.dto.ProductResponseDto;
import com.project.memberservice.entity.Cart;
import com.project.memberservice.entity.CartProduct;
import com.project.memberservice.entity.Member;
import com.project.memberservice.exception.CustomException;
import com.project.memberservice.exception.ErrorCode;
import com.project.memberservice.exception.FeignErrorDecoder;
import com.project.memberservice.repository.CartProductRepository;
import com.project.memberservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartProductRepository cartProductRepository;
    private final ProductServiceClient productServiceClient;
    private final FeignErrorDecoder feignErrorDecoder;

    /**
     * 장바구니 생성(회원가입)
     *
     * @param member 장바구니를 생성할 회원
     */
    @Transactional
    public void createCart(Member member) {
        Cart cart = new Cart();
        cart.setMember(member);
        cartRepository.save(cart);
    }

    /**
     * 장바구니 담기
     * - 해당 상품 없음 -> 상품 등록
     * - 해당 상품 있음 -> 상품 수량 수정
     *
     * @param id memberId
     * @param cartRequestDto 장바구니에 추가할 상품 목록
     */
    @Transactional
    public void addCart(String id, CartRequestDto cartRequestDto) {
        Long memberId = Long.parseLong(id);

        // 1. 장바구니 조회
        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        List<CartProductRequestDto> cartProducts = cartRequestDto.getCartProducts();
        for (CartProductRequestDto cartProductRequestDto : cartProducts) {
            Long productId = cartProductRequestDto.getProductId();
            int quantityToAdd = cartProductRequestDto.getQuantity(); // 추가할 수량

            // 2. 장바구니에 해당 상품 있는지 조회
            CartProduct cartProduct = cartProductRepository.findByCartAndProductId(cart, cartProductRequestDto.getProductId())
                    .orElse(null);  // 추가/증가 분기 처리를 위해 예외 던지지 않음

            // 3. 해당 상품의 재고 조회
            ProductResponseDto productResponseDto =
                    productServiceClient.getProductDetail(cartProductRequestDto.getProductId()).getBody();
            int availableStock = productResponseDto.getStock(); // 최대 추가 가능 수량

            if (availableStock<=0) {
                continue;   // 재고 0개인 상품은 추가하지 않음
            }
            // 추가할 수량이 재고 수량보다 많은 경우 조정
            if (quantityToAdd > availableStock) {
                log.info("재고 부족으로 인해 수량이 조정되었습니다. 요청: {}, 재고: {}", quantityToAdd, availableStock);
                quantityToAdd = availableStock;
            }

            if (cartProduct == null) {
                // 3. 장바구니에 해당 상품이 없으면 새로 추가
                log.info("장바구니에 상품이 없음 -> 새로 추가");
                cartProduct = new CartProduct(quantityToAdd, cart, productId);
            } else {
                // 4. 장바구니에 해당 상품이 있으면 수량 증가
                log.info("장바구니에 상품이 있음 -> 수량 변경");
                // 최종 장바구니 수량은 재고 수량을 초과할 수 없다.
                int newQuantity = Math.min(cartProduct.getQuantity() + quantityToAdd, availableStock);
                cartProduct.setQuantity(newQuantity);
            }

            cartProductRepository.save(cartProduct);
        }
    }

    /**
     * 장바구니 (수량)수정
     *
     * @param id memberId
     */
    public void updateQuantity(String id, CartRequestDto cartRequestDto, Boolean isIncrease) {
        Long memberId = Long.parseLong(id);
        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        if (isIncrease) {
            // 수량 증가 -> addCart 호출
            addCart(id, cartRequestDto);
        } else {
            // 수량 감소
            List<CartProductRequestDto> cartProducts = cartRequestDto.getCartProducts();    // 수정할 상품 목록
            List<CartProduct> updatedCartProducts = new ArrayList<>();  // 변경할 상품 목록
            List<CartProduct> productsToRemove = new ArrayList<>();     // 삭제할 상품 목록

            for (CartProductRequestDto cartProductRequestDto : cartProducts) {
                CartProduct cartProduct = cartProductRepository.findByCartAndProductId(cart, cartProductRequestDto.getProductId())
                        .orElseThrow(() -> new CustomException(ErrorCode.CART_PRODUCT_NOT_FOUND));

                int quantityChange = cartProductRequestDto.getQuantity();
                if (cartProduct.getQuantity() <= quantityChange) {
                    productsToRemove.add(cartProduct);  // 삭제 목록에 추가
                } else {
                    cartProduct.setQuantity(cartProduct.getQuantity() - quantityChange);
                    updatedCartProducts.add(cartProduct);  // 업데이트 목록에 추가
                }
            }

            // 변경된 상품 목록 저장
            if (!updatedCartProducts.isEmpty()) {
                cartProductRepository.saveAll(updatedCartProducts);
            }

            // 삭제할 상품이 있으면 실행
            if (!productsToRemove.isEmpty()) {
                cartProductRepository.deleteAll(productsToRemove);
            }
        }
    }

    /**
     * 장바구니 삭제
     *
     * @param id memberId
     * @param cartProductIdList 장바구니에서 삭제할 상품 목록
     */
    public void deleteProduct(String id, List<Long> cartProductIdList) {
        log.info("id: {}, productIds size: {}", id, cartProductIdList.size());

        Long memberId = Long.parseLong(id);
        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        List<CartProduct> cartProducts = cartProductRepository.findByCartAndProductIdIn(cart, cartProductIdList);

        if (cartProducts.size() != cartProductIdList.size()) {
            throw new CustomException(ErrorCode.CART_PRODUCT_NOT_FOUND);
        }

        cartProductRepository.deleteAll(cartProducts);
    }

    /**
     * 장바구니 조회
     *
     * @param id memberId
     * @return List<ProductResponseDto> 장바구니에 담긴 상품 목록(상품 정보 포함)
     */
    public List<ProductResponseDto> getCart(String id) {
        Long memberId = Long.parseLong(id);

        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CART_NOT_FOUND));

        List<CartProduct> cartProducts = cartProductRepository.findAllByCart(cart);
        if (cartProducts.isEmpty()) {
            return new ArrayList<>();   //비어있는 List 반환
        }

        // 각 상품의 ID 목록 추출
        List<Long> productIds = cartProducts.stream()
                .map(CartProduct::getProductId)
                .collect(Collectors.toList());

        ProductIdsRequestDto productIdsRequestDto = new ProductIdsRequestDto();
        productIdsRequestDto.setProductIds(productIds);

        // 여러 상품의 상세 조회
        List<ProductResponseDto> productDetails = productServiceClient.getProductsDetails(productIdsRequestDto).getBody();

        return productDetails;
    }

}
