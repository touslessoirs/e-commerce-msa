package com.project.memberservice.repository;

import com.project.memberservice.entity.Cart;
import com.project.memberservice.entity.CartProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartProductRepository extends JpaRepository<CartProduct, Long> {

    /* 장바구니에서 해당 상품 조회 */
    Optional<CartProduct> findByCartAndProductId(Cart cart, Long productId);

    /* 장바구니에 담긴 상품 목록 조회 */
    List<CartProduct> findAllByCart(Cart cart);

    /* 해당하는 모든 CartProduct를 한 번에 조회 */
    List<CartProduct> findByCartAndProductIdIn(Cart cart, List<Long> cartProductIdList);
}
