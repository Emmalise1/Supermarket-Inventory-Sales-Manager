package com.supermarket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.dto.ReportDtos.DashboardDto;
import com.supermarket.repository.ProductRepository;
import com.supermarket.repository.SaleRepository;
import com.supermarket.security.AuthContext;
import com.supermarket.security.AuthContext.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Dashboard summary with a short-lived Redis cache
 * ({@code dashboard:branch:{branchId}}, TTL 5 minutes).
 *
 * <p>The cache only stores non-sensitive aggregate counters - never
 * credentials or personal data.</p>
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final ProductCacheService cache;
    private final AuthContext authContext;
    private final ObjectMapper objectMapper;

    public DashboardService(ProductRepository productRepository,
                            SaleRepository saleRepository,
                            ProductCacheService cache,
                            AuthContext authContext,
                            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.cache = cache;
        this.authContext = authContext;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DashboardDto summary(Long branchId) {
        AuthUser user = authContext.require();
        Long scope = user.isAdmin() ? branchId : user.branchId();
        String key = ProductCacheService.dashboardKey(scope);

        // 1. Check Redis.
        Optional<String> cached = cache.getJson(key);
        if (cached.isPresent()) {
            try {
                DashboardDto dto = objectMapper.readValue(cached.get(), DashboardDto.class);
                return new DashboardDto(dto.branchId(), dto.productCount(), dto.lowStockCount(),
                        dto.todayRevenue(), dto.todaySales(), true, dto.recentSaleNumbers());
            } catch (JsonProcessingException ex) {
                cache.evictKey(key);
            }
        }

        // 2. Cache miss -> MySQL.
        long productCount = scope == null
                ? productRepository.countByActiveTrue()
                : productRepository.countByBranchIdAndActiveTrue(scope);
        long lowStockCount = (scope == null
                ? productRepository.findLowStock()
                : productRepository.findLowStockByBranch(scope)).size();

        Instant from = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = Instant.now();
        BigDecimal revenue = scope == null
                ? saleRepository.sumRevenueBetween(from, to)
                : saleRepository.sumRevenueBetweenForBranch(scope, from, to);
        long sales = scope == null
                ? saleRepository.countByCreatedAtBetween(from, to)
                : saleRepository.countByBranchIdAndCreatedAtBetween(scope, from, to);
        List<String> recent = (scope == null
                ? saleRepository.findTop50ByOrderByCreatedAtDesc()
                : saleRepository.findByBranchIdOrderByCreatedAtDesc(scope)).stream()
                .limit(5)
                .map(s -> s.getSaleNumber() + " (" + s.getTotalAmount() + ")")
                .toList();

        DashboardDto dto = new DashboardDto(scope, productCount, lowStockCount,
                revenue, sales, false, recent);

        // 3. Store in Redis with TTL for the next requests.
        try {
            cache.putJson(key, objectMapper.writeValueAsString(dto), cache.getDashboardTtl());
        } catch (JsonProcessingException ex) {
            log.warn("Could not cache dashboard {}", key, ex);
        }
        return dto;
    }
}
