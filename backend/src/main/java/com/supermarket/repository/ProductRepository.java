package com.supermarket.repository;

import com.supermarket.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    List<Product> findByBranchIdOrderByUpdatedAtDesc(Long branchId);

    List<Product> findAllByOrderByUpdatedAtDesc();

    /** Low-stock products: active products at or below their threshold. */
    @Query("select p from Product p where p.active = true and p.quantityInStock <= p.lowStockThreshold")
    List<Product> findLowStock();

    @Query("select p from Product p where p.active = true and p.quantityInStock <= p.lowStockThreshold and p.branchId = :branchId")
    List<Product> findLowStockByBranch(@Param("branchId") Long branchId);

    long countByActiveTrue();

    long countByBranchIdAndActiveTrue(Long branchId);
}
