package com.supermarket.repository;

import com.supermarket.domain.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    /** Aggregation for reports: top selling products in a date range. */
    @Query("select i.productId, i.productName, sum(i.quantity), sum(i.subtotal) from SaleItem i " +
            "where i.sale.createdAt >= :from and i.sale.createdAt < :to " +
            "group by i.productId, i.productName order by sum(i.quantity) desc")
    List<Object[]> topProductsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select i.productId, i.productName, sum(i.quantity), sum(i.subtotal) from SaleItem i " +
            "where i.sale.branchId = :branchId and i.sale.createdAt >= :from and i.sale.createdAt < :to " +
            "group by i.productId, i.productName order by sum(i.quantity) desc")
    List<Object[]> topProductsBetweenForBranch(@Param("branchId") Long branchId,
                                               @Param("from") Instant from, @Param("to") Instant to);
}
