package com.supermarket.web;

import com.supermarket.dto.ReportDtos.DashboardDto;
import com.supermarket.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Dashboard summary (cached in Redis for 5 minutes). */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardDto summary(@RequestParam(required = false) Long branchId) {
        return dashboardService.summary(branchId);
    }
}
