package com.supermarket.service;

import com.supermarket.config.RabbitMQConfig;
import com.supermarket.domain.MovementType;
import com.supermarket.domain.Product;
import com.supermarket.domain.StockMovement;
import com.supermarket.dto.InventoryDtos.GoodsReceiptRequest;
import com.supermarket.dto.InventoryDtos.StockAdjustmentRequest;
import com.supermarket.dto.InventoryDtos.StockMovementDto;
import com.supermarket.dto.ProductDtos.ProductDto;
import com.supermarket.exception.BusinessRuleException;
import com.supermarket.exception.ResourceNotFoundException;
import com.supermarket.mq.DomainEvent;
import com.supermarket.mq.EventPublisher;
import com.supermarket.repository.ProductRepository;
import com.supermarket.repository.StockMovementRepository;
import com.supermarket.security.AuthContext;
import com.supermarket.security.AuthContext.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Inventory operations: goods receiving (+stock), stock adjustments (+/-)
 * and the movement audit trail. Every change updates MySQL first and then
 * invalidates the Redis product cache.
 */
@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductCacheService cache;
    private final AuthContext authContext;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;
    private final LowStockNotifier lowStockNotifier;

    public InventoryService(ProductRepository productRepository,
                            StockMovementRepository stockMovementRepository,
                            ProductCacheService cache,
                            AuthContext authContext,
                            AuditService auditService,
                            EventPublisher eventPublisher,
                            LowStockNotifier lowStockNotifier) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.cache = cache;
        this.authContext = authContext;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.lowStockNotifier = lowStockNotifier;
    }

    /** POST /api/inventory/goods-receiving - supplier delivery adds stock. */
    @Transactional
    public ProductDto receiveGoods(GoodsReceiptRequest request) {
        AuthUser user = authContext.require();
        Product product = requireProduct(request.productId());
        authContext.requireBranchAccess(product.getBranchId());

        if (request.quantity() <= 0) {
            throw new BusinessRuleException("Received quantity must be at least 1");
        }

        product.setQuantityInStock(product.getQuantityInStock() + request.quantity());
        product.setUpdatedAt(Instant.now());
        product = productRepository.saveAndFlush(product);

        stockMovementRepository.save(new StockMovement(
                MovementType.GOODS_RECEIPT, product.getId(), product.getBranchId(),
                request.quantity(),
                trim("Received " + request.quantity() + " unit(s)"
                        + (isBlank(request.reference()) ? "" : " ref " + request.reference())
                        + (isBlank(request.note()) ? "" : " - " + request.note())),
                user.id()));

        cache.evictProduct(product);

        auditService.record("GOODS_RECEIVED", "Product", product.getId(),
                "Received " + request.quantity() + " unit(s) of '" + product.getName() + "'");

        eventPublisher.publish(RabbitMQConfig.RK_GOODS_RECEIVED, DomainEvent.of(
                "GOODS_RECEIVED", product.getBranchId(), user.id(),
                "Goods received: " + product.getName(),
                request.quantity() + " unit(s) of '" + product.getName() + "' added. Stock is now "
                        + product.getQuantityInStock() + "."));

        lowStockNotifier.check(product);

        return ProductDto.from(product);
    }

    /** POST /api/inventory/adjustments - manual correction, may never go negative. */
    @Transactional
    public ProductDto adjustStock(StockAdjustmentRequest request) {
        AuthUser user = authContext.require();
        Product product = requireProduct(request.productId());
        authContext.requireBranchAccess(product.getBranchId());

        if (request.quantityChange() == 0) {
            throw new BusinessRuleException("Adjustment must be non-zero");
        }
        int newStock = product.getQuantityInStock() + request.quantityChange();
        if (newStock < 0) {
            throw new BusinessRuleException(
                    "Adjustment would make stock negative for '" + product.getName()
                            + "': current " + product.getQuantityInStock()
                            + ", change " + request.quantityChange());
        }

        product.setQuantityInStock(newStock);
        product.setUpdatedAt(Instant.now());
        product = productRepository.saveAndFlush(product);

        stockMovementRepository.save(new StockMovement(
                MovementType.ADJUSTMENT, product.getId(), product.getBranchId(),
                request.quantityChange(), request.reason(), user.id()));

        cache.evictProduct(product);

        auditService.record("STOCK_ADJUSTED", "Product", product.getId(),
                "Adjusted '" + product.getName() + "' by " + request.quantityChange()
                        + " (" + request.reason() + ")");

        eventPublisher.publish(RabbitMQConfig.RK_STOCK_ADJUSTED, DomainEvent.of(
                "STOCK_ADJUSTED", product.getBranchId(), user.id(),
                "Stock adjusted: " + product.getName(),
                "Stock of '" + product.getName() + "' changed by " + request.quantityChange()
                        + " (" + request.reason() + "). New stock: " + newStock + "."));

        lowStockNotifier.check(product);

        return ProductDto.from(product);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> movements(Long branchId) {
        AuthUser user = authContext.require();
        Long scope = user.isAdmin() ? branchId : user.branchId();
        List<StockMovement> movements = scope == null
                ? stockMovementRepository.findTop100ByOrderByCreatedAtDesc()
                : stockMovementRepository.findTop100ByBranchIdOrderByCreatedAtDesc(scope);

        List<Long> productIds = movements.stream().map(StockMovement::getProductId).distinct().toList();
        Map<Long, String> names = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        List<StockMovementDto> result = new ArrayList<>();
        for (StockMovement movement : movements) {
            Product product = productRepository.findById(movement.getProductId()).orElse(null);
            result.add(StockMovementDto.from(movement,
                    names.getOrDefault(movement.getProductId(), "unknown"),
                    product == null ? 0 : product.getQuantityInStock()));
        }
        return result;
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
