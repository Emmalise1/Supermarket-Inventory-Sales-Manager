package com.supermarket.repository;

import com.supermarket.domain.MovementType;
import com.supermarket.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findTop100ByOrderByCreatedAtDesc();

    List<StockMovement> findTop100ByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    long countByMovementType(MovementType type);
}
