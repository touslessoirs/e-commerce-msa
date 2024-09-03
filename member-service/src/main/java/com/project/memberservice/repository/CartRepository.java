package com.project.memberservice.repository;

import com.project.memberservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    /* 해당 회원의 장바구니 조회 */
    Optional<Cart> findByMember_MemberId(Long userId);
}