package com.supermarket.repository;

import com.supermarket.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findTop50ByOrderByCreatedAtDesc();

    List<Sale> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s " +
            "where s.createdAt >= :from and s.createdAt < :to")
    BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s " +
            "where s.branchId = :branchId and s.createdAt >= :from and s.createdAt < :to")
    BigDecimal sumRevenueBetweenForBranch(@Param("branchId") Long branchId,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);

    long countByCreatedAtBetween(Instant from, Instant to);

    long countByBranchIdAndCreatedAtBetween(Long branchId, Instant from, Instant to);
}
