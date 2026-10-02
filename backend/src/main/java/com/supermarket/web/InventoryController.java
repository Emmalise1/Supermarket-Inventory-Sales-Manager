package com.supermarket.web;

import com.supermarket.dto.InventoryDtos.GoodsReceiptRequest;
import com.supermarket.dto.InventoryDtos.StockAdjustmentRequest;
import com.supermarket.dto.InventoryDtos.StockMovementDto;
import com.supermarket.dto.ProductDtos.ProductDto;
import com.supermarket.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inventory: goods receiving, stock adjustments, movement history. */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/goods-receiving")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductDto receiveGoods(@Valid @RequestBody GoodsReceiptRequest request) {
        return inventoryService.receiveGoods(request);
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductDto adjust(@Valid @RequestBody StockAdjustmentRequest request) {
        return inventoryService.adjustStock(request);
    }

    @GetMapping("/movements")
    public List<StockMovementDto> movements(@RequestParam(required = false) Long branchId) {
        return inventoryService.movements(branchId);
    }
}
