package com.supermarket.web;

import com.supermarket.dto.ReportDtos.SalesReportDto;
import com.supermarket.dto.ReportDtos.TrendPointDto;
import com.supermarket.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sales reports (manager/admin only). */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SalesReportDto sales(@RequestParam(required = false) String from,
                                @RequestParam(required = false) String to,
                                @RequestParam(required = false) Long branchId) {
        return reportService.salesReport(from, to, branchId);
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<TrendPointDto> trend(@RequestParam(defaultValue = "7") int days) {
        return reportService.trend(days);
    }
}
