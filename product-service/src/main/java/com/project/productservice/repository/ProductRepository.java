package com.project.productservice.repository;

import com.project.productservice.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    /* Pessimistic Lock */
//    @Transactional(readOnly = false)
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    Optional<Product> findById(Long productId);

//    @Query("SELECT CASE WHEN p.stock >= :quantity THEN TRUE ELSE FALSE END FROM Product p WHERE p.id = :productId")
//    boolean existsByIdAndStockGreaterThanEqual(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Transactional(readOnly = false)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}
