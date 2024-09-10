package com.project.productservice.repository;

import com.project.productservice.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 상품 조회 시 비관적 락 걸기
     * 
     * @param productId
     * @return
     */
    @Transactional(readOnly = false)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdWithLock(@Param("productId") Long productId);

    /**
     * 전체 상품 조회 + 페이징 처리
     * 
     * @param pageable
     * @return
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * 카테고리별 상품 조회 + 페이징 처리
     * 
     * @param category 해당 카테고리에 속하는 상품을 필터링
     * @param pageable
     * @return 해당 카테고리에 해당하는 상품 목록
     */
    Page<Product> findByCategory(String category, Pageable pageable);
}
