package com.supermarket.service;

import com.supermarket.dto.ReportDtos.SalesReportDto;
import com.supermarket.dto.ReportDtos.TopProductDto;
import com.supermarket.dto.ReportDtos.TrendPointDto;
import com.supermarket.repository.SaleItemRepository;
import com.supermarket.repository.SaleRepository;
import com.supermarket.security.AuthContext;
import com.supermarket.security.AuthContext.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Sales reports (always read from MySQL - reporting needs fresh data). */
@Service
public class ReportService {

    private static final int TOP_LIMIT = 5;

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final AuthContext authContext;

    public ReportService(SaleRepository saleRepository,
                         SaleItemRepository saleItemRepository,
                         AuthContext authContext) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.authContext = authContext;
    }

    @Transactional(readOnly = true)
    public SalesReportDto salesReport(String fromStr, String toStr, Long branchId) {
        AuthUser user = authContext.require();
        Long scope = user.isAdmin() ? branchId : user.branchId();

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate fromDate = parseOrDefault(fromStr, today.minusDays(6));
        LocalDate toDate = parseOrDefault(toStr, today);

        Instant from = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        BigDecimal revenue = scope == null
                ? saleRepository.sumRevenueBetween(from, to)
                : saleRepository.sumRevenueBetweenForBranch(scope, from, to);
        long count = scope == null
                ? saleRepository.countByCreatedAtBetween(from, to)
                : saleRepository.countByBranchIdAndCreatedAtBetween(scope, from, to);

        BigDecimal average = count == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        List<Object[]> rows = scope == null
                ? saleItemRepository.topProductsBetween(from, to)
                : saleItemRepository.topProductsBetweenForBranch(scope, from, to);

        List<TopProductDto> top = new ArrayList<>();
        for (Object[] row : rows) {
            if (top.size() >= TOP_LIMIT) {
                break;
            }
            top.add(new TopProductDto(
                    (Long) row[0],
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    (BigDecimal) row[3]));
        }

        return new SalesReportDto(
                fromDate.toString(), toDate.toString(), scope, count, revenue, average, top);
    }

    @Transactional(readOnly = true)
    public List<TrendPointDto> trend(int days) {
        int window = Math.max(1, Math.min(days, 90));
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = today.minusDays(window - 1L);
        Instant from = start.atStartOfDay(zone).toInstant();

        Map<LocalDate, BigDecimal> revenueByDay = new HashMap<>();
        Map<LocalDate, Long> countByDay = new HashMap<>();
        for (Object[] row : saleRepository.dailyRevenueSince(from)) {
            LocalDate date = toLocalDate(row[0]);
            if (date == null) {
                continue;
            }
            revenueByDay.put(date, toBigDecimal(row[1]));
            countByDay.put(date, row[2] == null ? 0L : ((Number) row[2]).longValue());
        }

        List<TrendPointDto> points = new ArrayList<>(window);
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            points.add(new TrendPointDto(date.toString(),
                    revenueByDay.getOrDefault(date, BigDecimal.ZERO),
                    countByDay.getOrDefault(date, 0L)));
        }
        return points;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static LocalDate parseOrDefault(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }
}
