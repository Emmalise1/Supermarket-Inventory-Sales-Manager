package com.supermarket.web;

import com.supermarket.dto.ProductDtos.BarcodeBenchmarkResponse;
import com.supermarket.dto.ProductDtos.BarcodeLookupResponse;
import com.supermarket.dto.ProductDtos.ProductDto;
import com.supermarket.dto.ProductDtos.ProductRequest;
import com.supermarket.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Product API. The barcode endpoint is the Redis caching showcase:
 * cache hit -&gt; answered from Redis, cache miss -&gt; MySQL then cached.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) Long branchId) {
        return productService.list(branchId);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<ProductDto> lowStock(@RequestParam(required = false) Long branchId) {
        return productService.lowStock(branchId);
    }

    /** Product by id - cached in Redis under product:id:{id}. */
    @GetMapping("/{id}")
    public ProductDto byId(@PathVariable Long id) {
        return productService.getByIdCached(id);
    }

    /** Redis-backed barcode lookup (see README "Redis caching"). */
    @GetMapping("/barcode/{barcode}")
    public BarcodeLookupResponse byBarcode(@PathVariable String barcode) {
        return productService.lookupByBarcode(barcode);
    }

    /** Measured performance comparison: MySQL lookup vs Redis lookup. */
    @GetMapping("/barcode/{barcode}/benchmark")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BarcodeBenchmarkResponse benchmark(@PathVariable String barcode,
                                              @RequestParam(defaultValue = "20") int iterations) {
        return productService.benchmark(barcode, Math.max(1, Math.min(iterations, 200)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ProductDto deactivate(@PathVariable Long id) {
        return productService.deactivate(id);
    }
}
