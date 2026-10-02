package com.supermarket.web;

import com.supermarket.dto.SupplierDtos.SupplierDto;
import com.supermarket.dto.SupplierDtos.SupplierRequest;
import com.supermarket.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Supplier management. */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final CatalogService catalogService;

    public SupplierController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<SupplierDto> list() {
        return catalogService.listSuppliers();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SupplierDto create(@Valid @RequestBody SupplierRequest request) {
        return catalogService.createSupplier(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return catalogService.updateSupplier(id, request);
    }
}
