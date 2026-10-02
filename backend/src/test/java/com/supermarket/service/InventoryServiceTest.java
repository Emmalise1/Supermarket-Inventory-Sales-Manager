package com.supermarket.service;

import com.supermarket.TestAuth;
import com.supermarket.domain.Product;
import com.supermarket.domain.Role;
import com.supermarket.dto.InventoryDtos.GoodsReceiptRequest;
import com.supermarket.dto.InventoryDtos.StockAdjustmentRequest;
import com.supermarket.exception.BusinessRuleException;
import com.supermarket.mq.EventPublisher;
import com.supermarket.repository.ProductRepository;
import com.supermarket.repository.StockMovementRepository;
import com.supermarket.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Goods receiving adds stock; adjustments may never make stock negative. */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductCacheService cache;
    @Mock private AuditService auditService;
    @Mock private EventPublisher eventPublisher;
    @Mock private LowStockNotifier lowStockNotifier;

    private final AuthContext authContext = new AuthContext();
    private InventoryService inventoryService;
    private Product product;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(productRepository, stockMovementRepository,
                cache, authContext, auditService, eventPublisher, lowStockNotifier);

        TestAuth.authenticate(2L, "manager@supermarket.rw", Role.MANAGER, 1L);

        product = new Product();
        product.setId(1L);
        product.setBarcode("6001000000017");
        product.setName("Sugar 1kg");
        product.setBranchId(1L);
        product.setQuantityInStock(5);
        product.setLowStockThreshold(10);
        product.setActive(true);

        lenient().when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        lenient().when(productRepository.saveAndFlush(any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    @Test
    void goodsReceiving_addsStock_andInvalidatesCache() {
        inventoryService.receiveGoods(new GoodsReceiptRequest(1L, 12, "PO-001", null));

        assertThat(product.getQuantityInStock()).isEqualTo(17); // 5 + 12
        verify(stockMovementRepository).save(any());
        verify(cache).evictProduct(product);
        verify(eventPublisher).publish(any(), any());
    }

    @Test
    void negativeAdjustment_thatWouldZeroStock_isRejected() {
        assertThatThrownBy(() -> inventoryService.adjustStock(
                new StockAdjustmentRequest(1L, -9, "Damaged goods")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negative");

        assertThat(product.getQuantityInStock()).isEqualTo(5); // unchanged
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void validAdjustment_appliesChange() {
        inventoryService.adjustStock(new StockAdjustmentRequest(1L, -2, "Expired"));

        assertThat(product.getQuantityInStock()).isEqualTo(3);
        verify(stockMovementRepository).save(any());
        verify(cache).evictProduct(product);
    }

    @Test
    void zeroAdjustment_isRejected() {
        assertThatThrownBy(() -> inventoryService.adjustStock(
                new StockAdjustmentRequest(1L, 0, "no-op")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void otherBranchProduct_isForbidden() {
        product.setBranchId(99L);

        assertThatThrownBy(() -> inventoryService.receiveGoods(new GoodsReceiptRequest(1L, 5, null, null)))
                .isInstanceOf(com.supermarket.exception.ForbiddenException.class);
    }
}
