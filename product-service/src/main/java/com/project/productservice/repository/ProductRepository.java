package com.project.productservice.repository;

import com.project.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    /* Pessimistic Lock */
//    @Transactional(readOnly = false)
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    Optional<Product> findById(Long productId);
}
