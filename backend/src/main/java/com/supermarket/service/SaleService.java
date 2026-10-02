package com.supermarket.service;

import com.supermarket.config.RabbitMQConfig;
import com.supermarket.domain.MovementType;
import com.supermarket.domain.Product;
import com.supermarket.domain.Sale;
import com.supermarket.domain.SaleItem;
import com.supermarket.domain.StockMovement;
import com.supermarket.dto.SaleDtos.SaleDto;
import com.supermarket.dto.SaleDtos.SaleRequest;
import com.supermarket.exception.BusinessRuleException;
import com.supermarket.exception.ResourceNotFoundException;
import com.supermarket.mq.DomainEvent;
import com.supermarket.mq.EventPublisher;
import com.supermarket.repository.ProductRepository;
import com.supermarket.repository.SaleItemRepository;
import com.supermarket.repository.SaleRepository;
import com.supermarket.repository.StockMovementRepository;
import com.supermarket.security.AuthContext;
import com.supermarket.security.AuthContext.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * POS sale processing with transactional stock deduction.
 *
 * <p>Business rules enforced here:</p>
 * <ul>
 *   <li>Prices always come from MySQL, never from the client.</li>
 *   <li>Stock can never go negative - the sale is rejected instead.</li>
 *   <li>Every sale writes stock movements (audit trail).</li>
 *   <li>Affected product caches are invalidated because stock changed.</li>
 *   <li>Low stock triggers a RabbitMQ notification event.</li>
 * </ul>
 */
@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductCacheService cache;
    private final AuthContext authContext;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;
    private final LowStockNotifier lowStockNotifier;

    public SaleService(SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository,
                       ProductRepository productRepository,
                       StockMovementRepository stockMovementRepository,
                       ProductCacheService cache,
                       AuthContext authContext,
                       AuditService auditService,
                       EventPublisher eventPublisher,
                       LowStockNotifier lowStockNotifier) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.cache = cache;
        this.authContext = authContext;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.lowStockNotifier = lowStockNotifier;
    }

    @Transactional
    public SaleDto create(SaleRequest request) {
        AuthUser user = authContext.require();
        Long branchId = request.branchId() != null ? request.branchId() : user.branchId();
        authContext.requireBranchAccess(branchId);
        if (branchId == null) {
            throw new BusinessRuleException("A branch is required to record a sale");
        }

        // Merge duplicate product lines: productId -> total quantity.
        Map<Long, Integer> wanted = new HashMap<>();
        request.items().forEach(item -> wanted.merge(item.productId(), item.quantity(), Integer::sum));

        // Load and validate every product, then check stock BEFORE any change.
        List<Product> products = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product " + entry.getKey() + " not found"));
            if (!product.isActive()) {
                throw new BusinessRuleException("Product '" + product.getName() + "' is deactivated");
            }
            if (!product.getBranchId().equals(branchId)) {
                throw new BusinessRuleException(
                        "Product '" + product.getName() + "' does not belong to branch " + branchId);
            }
            int quantity = entry.getValue();
            if (product.getQuantityInStock() < quantity) {
                throw new BusinessRuleException(
                        "Insufficient stock for '" + product.getName() + "': requested " + quantity
                                + ", available " + product.getQuantityInStock());
            }
            products.add(product);
        }

        // Header
        Sale sale = new Sale();
        sale.setSaleNumber("S-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        sale.setBranchId(branchId);
        sale.setCashierId(user.id());
        sale.setCreatedAt(Instant.now());

        BigDecimal total = BigDecimal.ZERO;
        int itemCount = 0;
        List<SaleItem> items = new ArrayList<>();

        for (Product product : products) {
            int quantity = wanted.get(product.getId());
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(unitPrice);
            item.setQuantity(quantity);
            item.setSubtotal(subtotal);
            items.add(item);

            // Stock deduction - can never go below zero (validated above).
            product.setQuantityInStock(product.getQuantityInStock() - quantity);
            product.setUpdatedAt(Instant.now());

            total = total.add(subtotal);
            itemCount += quantity;

            stockMovementRepository.save(new StockMovement(
                    MovementType.SALE, product.getId(), branchId, -quantity,
                    "Sale " + sale.getSaleNumber(), user.id()));
        }

        sale.setTotalAmount(total);
        sale.setItemCount(itemCount);
        sale = saleRepository.saveAndFlush(sale);
        saleItemRepository.saveAll(items);
        productRepository.saveAll(products);

        // Stock changed: cached product entries would be stale -> invalidate.
        products.forEach(cache::evictProduct);

        auditService.record("SALE_COMPLETED", "Sale", sale.getId(),
                "Sale " + sale.getSaleNumber() + " totaling " + total + " with " + itemCount + " item(s)");

        eventPublisher.publish(RabbitMQConfig.RK_SALE_COMPLETED, DomainEvent.of(
                "SALE_COMPLETED", branchId, user.id(),
                "Sale " + sale.getSaleNumber() + " completed",
                "Sale of " + itemCount + " item(s) worth " + total + " was recorded."));

        products.forEach(lowStockNotifier::check);

        return SaleDto.from(sale, items);
    }

    @Transactional(readOnly = true)
    public List<SaleDto> list(Long branchId) {
        AuthUser user = authContext.require();
        Long scope = user.isAdmin() ? branchId : user.branchId();
        List<Sale> sales = scope == null
                ? saleRepository.findTop50ByOrderByCreatedAtDesc()
                : saleRepository.findByBranchIdOrderByCreatedAtDesc(scope);
        return sales.stream()
                .map(sale -> SaleDto.from(sale, saleItemRepository.findBySaleId(sale.getId())))
                .toList();
    }
}
