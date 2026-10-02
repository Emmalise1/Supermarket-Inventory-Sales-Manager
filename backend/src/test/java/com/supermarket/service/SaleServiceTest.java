package com.supermarket.service;

import com.supermarket.TestAuth;
import com.supermarket.config.RabbitMQConfig;
import com.supermarket.domain.MovementType;
import com.supermarket.domain.Product;
import com.supermarket.domain.Role;
import com.supermarket.domain.StockMovement;
import com.supermarket.dto.SaleDtos.SaleDto;
import com.supermarket.dto.SaleDtos.SaleItemRequest;
import com.supermarket.dto.SaleDtos.SaleRequest;
import com.supermarket.exception.BusinessRuleException;
import com.supermarket.exception.ForbiddenException;
import com.supermarket.mq.EventPublisher;
import com.supermarket.repository.ProductRepository;
import com.supermarket.repository.SaleItemRepository;
import com.supermarket.repository.SaleRepository;
import com.supermarket.repository.StockMovementRepository;
import com.supermarket.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** POS business rules: stock deduction, no negative stock, branch scoping. */
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private SaleItemRepository saleItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private ProductCacheService cache;
    @Mock private AuditService auditService;
    @Mock private EventPublisher eventPublisher;
    @Mock private LowStockNotifier lowStockNotifier;

    private final AuthContext authContext = new AuthContext();
    private SaleService saleService;
    private Product product;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(saleRepository, saleItemRepository, productRepository,
                stockMovementRepository, cache, authContext, auditService, eventPublisher, lowStockNotifier);

        // Cashier assigned to branch 1 - branch-level authorization is real here.
        TestAuth.authenticate(10L, "cashier@supermarket.rw", Role.CASHIER, 1L);

        product = new Product();
        product.setId(1L);
        product.setBarcode("6001000000017");
        product.setName("Sugar 1kg");
        product.setBranchId(1L);
        product.setPrice(new BigDecimal("600.00"));
        product.setQuantityInStock(40);
        product.setLowStockThreshold(10);
        product.setActive(true);

        lenient().when(saleRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    private SaleRequest request(int quantity) {
        return new SaleRequest(1L, List.of(new SaleItemRequest(1L, quantity)));
    }

    @Test
    void createSale_deductsStock_recordsMovement_andEvictsCache() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        SaleDto dto = saleService.create(request(3));

        assertThat(product.getQuantityInStock()).isEqualTo(37); // 40 - 3
        assertThat(dto.totalAmount()).isEqualByComparingTo("1800.00"); // 3 x 600

        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getMovementType()).isEqualTo(MovementType.SALE);
        assertThat(movement.getValue().getQuantityChange()).isEqualTo(-3);

        verify(cache).evictProduct(product); // stale cached stock must be dropped
        verify(eventPublisher).publish(eq(RabbitMQConfig.RK_SALE_COMPLETED), any());
        verify(productRepository).saveAll(any());
    }

    @Test
    void createSale_insufficientStock_rejected_andStockUnchanged() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> saleService.create(request(999)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");

        assertThat(product.getQuantityInStock()).isEqualTo(40); // unchanged
        verify(productRepository, never()).saveAll(any());
        verify(saleRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publish(anyString(), any());
    }

    @Test
    void createSale_otherBranch_forbidden() {
        SaleRequest otherBranch = new SaleRequest(2L, List.of(new SaleItemRequest(1L, 1)));

        assertThatThrownBy(() -> saleService.create(otherBranch))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createSale_lowStock_publishesLowStockEvent() {
        product.setQuantityInStock(11);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        saleService.create(request(2)); // stock drops 11 -> 9 (<= threshold 10)

        assertThat(product.getQuantityInStock()).isEqualTo(9);
        verify(lowStockNotifier).check(product);
    }
}
