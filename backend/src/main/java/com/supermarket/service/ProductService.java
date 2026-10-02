package com.supermarket.service;

import com.supermarket.domain.Product;
import com.supermarket.dto.ProductDtos.BarcodeBenchmarkResponse;
import com.supermarket.dto.ProductDtos.BarcodeLookupResponse;
import com.supermarket.dto.ProductDtos.ProductDto;
import com.supermarket.dto.ProductDtos.ProductRequest;
import com.supermarket.exception.BusinessRuleException;
import com.supermarket.exception.DuplicateResourceException;
import com.supermarket.exception.ResourceNotFoundException;
import com.supermarket.repository.ProductRepository;
import com.supermarket.security.AuthContext;
import com.supermarket.security.AuthContext.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Product CRUD plus the Redis-backed barcode lookup.
 *
 * <p>Barcode lookup flow (the main Redis use case):</p>
 * <pre>
 * Receive barcode -&gt; Check Redis
 *   -&gt; HIT : return cached product (MySQL not queried)
 *   -&gt; MISS: query MySQL -&gt; store in Redis (TTL) -&gt; return product
 * </pre>
 *
 * <p>MySQL stays the source of truth: every write updates MySQL first and
 * <b>then invalidates</b> the affected Redis keys (including the old and the
 * new barcode when a barcode changes).</p>
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductCacheService cache;
    private final AuthContext authContext;
    private final AuditService auditService;

    public ProductService(ProductRepository productRepository,
                          ProductCacheService cache,
                          AuthContext authContext,
                          AuditService auditService) {
        this.productRepository = productRepository;
        this.cache = cache;
        this.authContext = authContext;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ reads

    /**
     * GET /api/products/barcode/{barcode} - Redis first, MySQL on miss.
     */
    @Transactional(readOnly = true)
    public BarcodeLookupResponse lookupByBarcode(String barcode) {
        long start = System.nanoTime();

        var cached = cache.getProductByBarcode(barcode);
        if (cached.isPresent()) {
            long elapsed = elapsedMs(start);
            log.debug("Barcode {} served from Redis cache in {} ms", barcode, elapsed);
            return new BarcodeLookupResponse(ProductDto.from(cached.get()), true, elapsed);
        }

        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("No product with barcode " + barcode));

        // Store in Redis with TTL so the next lookup skips MySQL.
        // Both documented keys are populated: product:barcode:{barcode} and product:id:{id}.
        cache.putProductByBarcode(barcode, product);
        cache.putProductById(product.getId(), product);
        long elapsed = elapsedMs(start);
        return new BarcodeLookupResponse(ProductDto.from(product), false, elapsed);
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    /**
     * GET /api/products/{id} - second Redis use case (frequently accessed
     * product details): checks {@code product:id:{id}} before MySQL.
     */
    @Transactional(readOnly = true)
    public ProductDto getByIdCached(Long id) {
        var cached = cache.getProductById(id);
        if (cached.isPresent()) {
            authContext.requireBranchAccess(cached.get().getBranchId());
            return ProductDto.from(cached.get());
        }
        Product product = getById(id);
        authContext.requireBranchAccess(product.getBranchId());
        cache.putProductById(product.getId(), product);
        return ProductDto.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> list(Long branchId) {
        AuthUser user = authContext.require();
        Long scope = user.isAdmin() ? branchId : user.branchId();
        List<Product> products = scope == null
                ? productRepository.findAllByOrderByUpdatedAtDesc()
                : productRepository.findByBranchIdOrderByUpdatedAtDesc(scope);
        return products.stream().map(ProductDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> lowStock(Long branchId) {
        AuthUser user = authContext.require();
        // Always read from MySQL: stock levels must be fresh for correctness.
        List<Product> products = user.isAdmin() && branchId == null
                ? productRepository.findLowStock()
                : productRepository.findLowStockByBranch(user.isAdmin() ? branchId : user.branchId());
        return products.stream().map(ProductDto::from).toList();
    }

    // ----------------------------------------------------------------- writes

    @Transactional
    public ProductDto create(ProductRequest request) {
        AuthUser user = authContext.require();
        Long branchId = user.isAdmin() ? request.branchId() : user.branchId();
        if (branchId == null) {
            throw new BusinessRuleException("A branch is required for the new product");
        }
        if (productRepository.existsByBarcode(request.barcode().trim())) {
            throw new DuplicateResourceException("Barcode " + request.barcode() + " already exists");
        }

        Product product = new Product();
        product.setBarcode(request.barcode().trim());
        product.setName(request.name().trim());
        product.setCategoryId(request.categoryId());
        product.setSupplierId(request.supplierId());
        product.setBranchId(branchId);
        product.setCostPrice(request.costPrice());
        product.setPrice(request.price());
        product.setQuantityInStock(request.quantityInStock() == null ? 0 : request.quantityInStock());
        product.setLowStockThreshold(request.lowStockThreshold() == null ? 10 : request.lowStockThreshold());
        product.setActive(request.active() == null || request.active());
        product = productRepository.saveAndFlush(product);

        evict(product);
        auditService.record("PRODUCT_CREATED", "Product", product.getId(),
                "Created product '" + product.getName() + "' (" + product.getBarcode() + ")");
        return ProductDto.from(product);
    }

    /**
     * PUT /api/products/{id} - updates MySQL then invalidates the cache.
     * If the barcode changed, both the old and the new barcode keys are removed.
     */
    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product product = getById(id);
        authContext.requireBranchAccess(product.getBranchId());

        String oldBarcode = product.getBarcode();
        boolean priceChanged = product.getPrice().compareTo(request.price()) != 0;

        product.setBarcode(request.barcode().trim());
        product.setName(request.name().trim());
        product.setCategoryId(request.categoryId());
        product.setSupplierId(request.supplierId());
        if (request.costPrice() != null) {
            product.setCostPrice(request.costPrice());
        }
        product.setPrice(request.price());
        if (request.lowStockThreshold() != null) {
            product.setLowStockThreshold(request.lowStockThreshold());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        product.setUpdatedAt(java.time.Instant.now());
        product = productRepository.saveAndFlush(product);

        // --- Cache invalidation (MySQL already updated = source of truth) ---
        // evictProduct removes both product:id:{id} and product:barcode:{new}
        cache.evictProduct(product);
        if (!product.getBarcode().equals(oldBarcode)) {
            cache.evictBarcode(oldBarcode); // ...and also the old barcode key
        }

        auditService.record("PRODUCT_UPDATED", "Product", product.getId(),
                (priceChanged ? "Price changed to " + product.getPrice() + ". " : "")
                        + (product.getBarcode().equals(oldBarcode) ? "" : "Barcode changed from " + oldBarcode + " to " + product.getBarcode() + ". ")
                        + "Product '" + product.getName() + "' updated");
        return ProductDto.from(product);
    }

    /** Soft delete: product is deactivated, never physically removed. */
    @Transactional
    public ProductDto deactivate(Long id) {
        Product product = getById(id);
        authContext.requireBranchAccess(product.getBranchId());
        product.setActive(false);
        product.setUpdatedAt(java.time.Instant.now());
        product = productRepository.saveAndFlush(product);
        evict(product);
        auditService.record("PRODUCT_DEACTIVATED", "Product", product.getId(),
                "Deactivated product '" + product.getName() + "'");
        return ProductDto.from(product);
    }

    // ------------------------------------------------------------- benchmark

    /**
     * Real, measured comparison of the two lookup paths (no invented numbers):
     * WITH CACHE = Redis lookup, WITHOUT CACHE = direct MySQL query.
     */
    @Transactional(readOnly = true)
    public BarcodeBenchmarkResponse benchmark(String barcode, int iterations) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("No product with barcode " + barcode));

        // Path 1: WITHOUT cache - straight to MySQL every time.
        long dbTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            productRepository.findByBarcode(barcode);
            dbTotal += System.nanoTime() - start;
        }

        // Path 2: WITH cache - warm the cache, then read only from Redis.
        cache.putProductByBarcode(barcode, product);
        long redisTotal = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            cache.getProductByBarcode(barcode);
            redisTotal += System.nanoTime() - start;
        }

        double avgDb = (dbTotal / 1000000.0) / iterations;
        double avgRedis = (redisTotal / 1000000.0) / iterations;
        double speedup = avgRedis > 0 ? avgDb / avgRedis : 0;

        String note = "Measured on this machine with " + iterations
                + " iterations at " + java.time.Instant.now()
                + ". Times are real measurements and vary with machine load.";

        return new BarcodeBenchmarkResponse(barcode, iterations,
                round(avgDb), round(avgRedis), round(speedup), note);
    }

    // -------------------------------------------------------------- internals

    private void evict(Product product) {
        cache.evictProduct(product);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
