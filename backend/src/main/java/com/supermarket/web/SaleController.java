package com.supermarket.web;

import com.supermarket.dto.SaleDtos.SaleDto;
import com.supermarket.dto.SaleDtos.SaleRequest;
import com.supermarket.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** POS sales: create and list sales. */
@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public SaleDto create(@Valid @RequestBody SaleRequest request) {
        return saleService.create(request);
    }

    @GetMapping
    public List<SaleDto> list(@RequestParam(required = false) Long branchId) {
        return saleService.list(branchId);
    }
}
