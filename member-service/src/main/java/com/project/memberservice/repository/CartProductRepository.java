package com.project.memberservice.repository;

import com.project.memberservice.entity.Cart;
import com.project.memberservice.entity.CartProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartProductRepository extends JpaRepository<CartProduct, Long> {

    /**
     * 특정 장바구니에 담긴 특정 상품 정보 조회
     *
     * @param productId
     * @param cart
     * @return 해당 장바구니에 담긴 해당 상품의 정보
     */
    Optional<CartProduct> findByProductIdAndCart(Long productId, Cart cart);

    /**
     * 특정 장바구니에 담긴 전체 상품 목록 조회
     *
     * @param cart
     * @return 해당 장바구니에 담긴 전체 상품 목록의 정보
     */
    List<CartProduct> findAllByCart(Cart cart);

    /**
     * 특정 장바구니에 담긴 특정 상품 목록 정보 조회
     *
     * @param cartProductIdList 조회하려는 상품 ID 목록
     * @param cart
     * @return 해당 장바구니에 담긴 해당 상품 목록의 정보
     */
    List<CartProduct> findAllByProductIdInAndCart(List<Long> cartProductIdList, Cart cart);
}
