package com.supermarket.dto;

import java.math.BigDecimal;
import java.util.List;

/** DTOs for dashboard and reports. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record DashboardDto(
            Long branchId,
            long productCount,
            long lowStockCount,
            BigDecimal todayRevenue,
            long todaySales,
            boolean fromCache,
            List<String> recentSaleNumbers) {
    }

    public record SalesReportDto(
            String from,
            String to,
            Long branchId,
            long saleCount,
            BigDecimal totalRevenue,
            BigDecimal averageSale,
            List<TopProductDto> topProducts) {
    }

    public record TopProductDto(
            Long productId,
            String productName,
            long quantitySold,
            BigDecimal revenue) {
    }

    public record TrendPointDto(
            String date,
            BigDecimal revenue,
            long salesCount) {
    }
}
