package com.supermarket.service;

import com.supermarket.domain.Branch;
import com.supermarket.domain.Category;
import com.supermarket.domain.Supplier;
import com.supermarket.dto.BranchDtos.BranchDto;
import com.supermarket.dto.BranchDtos.BranchRequest;
import com.supermarket.dto.CategoryDtos;
import com.supermarket.dto.SupplierDtos.SupplierDto;
import com.supermarket.dto.SupplierDtos.SupplierRequest;
import com.supermarket.exception.ResourceNotFoundException;
import com.supermarket.repository.BranchRepository;
import com.supermarket.repository.CategoryRepository;
import com.supermarket.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD for branches, categories and suppliers (simple reference data). */
@Service
public class CatalogService {

    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    public CatalogService(BranchRepository branchRepository,
                          CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository,
                          AuditService auditService) {
        this.branchRepository = branchRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------- branches

    @Transactional(readOnly = true)
    public List<BranchDto> listBranches() {
        return branchRepository.findAll().stream().map(BranchDto::from).toList();
    }

    @Transactional
    public BranchDto createBranch(BranchRequest request) {
        Branch branch = new Branch(request.name().trim(), request.address(), request.phone());
        if (request.active() != null) {
            branch.setActive(request.active());
        }
        branch = branchRepository.save(branch);
        auditService.record("BRANCH_CREATED", "Branch", branch.getId(), "Created branch " + branch.getName());
        return BranchDto.from(branch);
    }

    @Transactional
    public BranchDto updateBranch(Long id, BranchRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch " + id + " not found"));
        branch.setName(request.name().trim());
        branch.setAddress(request.address());
        branch.setPhone(request.phone());
        if (request.active() != null) {
            branch.setActive(request.active());
        }
        branch = branchRepository.save(branch);
        auditService.record("BRANCH_UPDATED", "Branch", branch.getId(), "Updated branch " + branch.getName());
        return BranchDto.from(branch);
    }

    // ----------------------------------------------------------- categories

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryDto> listCategories() {
        return categoryRepository.findAll().stream().map(CategoryDtos.CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDtos.CategoryDto createCategory(CategoryDtos.CategoryRequest request) {
        Category category = new Category(request.name().trim());
        category = categoryRepository.save(category);
        return CategoryDtos.CategoryDto.from(category);
    }

    // ------------------------------------------------------------ suppliers

    @Transactional(readOnly = true)
    public List<SupplierDto> listSuppliers() {
        return supplierRepository.findAll().stream().map(SupplierDto::from).toList();
    }

    @Transactional
    public SupplierDto createSupplier(SupplierRequest request) {
        Supplier supplier = new Supplier(request.name().trim(), request.contactPerson(),
                request.phone(), request.email());
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
        supplier = supplierRepository.save(supplier);
        auditService.record("SUPPLIER_CREATED", "Supplier", supplier.getId(),
                "Created supplier " + supplier.getName());
        return SupplierDto.from(supplier);
    }

    @Transactional
    public SupplierDto updateSupplier(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier " + id + " not found"));
        supplier.setName(request.name().trim());
        supplier.setContactPerson(request.contactPerson());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        if (request.active() != null) {
            supplier.setActive(request.active());
        }
        supplier = supplierRepository.save(supplier);
        auditService.record("SUPPLIER_UPDATED", "Supplier", supplier.getId(),
                "Updated supplier " + supplier.getName());
        return SupplierDto.from(supplier);
    }
}
