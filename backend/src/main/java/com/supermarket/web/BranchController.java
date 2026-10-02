package com.supermarket.web;

import com.supermarket.dto.BranchDtos.BranchDto;
import com.supermarket.dto.BranchDtos.BranchRequest;
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

/** Branch management. Reads for every authenticated user, writes for ADMIN. */
@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final CatalogService catalogService;

    public BranchController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<BranchDto> list() {
        return catalogService.listBranches();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BranchDto create(@Valid @RequestBody BranchRequest request) {
        return catalogService.createBranch(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BranchDto update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
        return catalogService.updateBranch(id, request);
    }
}
