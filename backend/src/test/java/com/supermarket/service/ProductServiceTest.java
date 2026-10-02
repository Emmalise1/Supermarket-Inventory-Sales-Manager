package com.supermarket.service;

import com.supermarket.TestAuth;
import com.supermarket.domain.Product;
import com.supermarket.domain.Role;
import com.supermarket.dto.ProductDtos.BarcodeBenchmarkResponse;
import com.supermarket.dto.ProductDtos.BarcodeLookupResponse;
import com.supermarket.dto.ProductDtos.ProductDto;
import com.supermarket.dto.ProductDtos.ProductRequest;
import com.supermarket.exception.DuplicateResourceException;
import com.supermarket.exception.ResourceNotFoundException;
import com.supermarket.repository.ProductRepository;
import com.supermarket.security.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis caching behaviour of the barcode lookup:
 * cache hit skips MySQL, cache miss queries MySQL and caches,
 * and writes invalidate the affected keys.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductCacheService cache;
    @Mock
    private AuditService auditService;

    private final AuthContext authContext = new AuthContext();
    private ProductService productService;
    private Product product;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, cache, authContext, auditService);
        TestAuth.authenticate(1L, "manager@supermarket.rw", Role.MANAGER, 1L);

        product = new Product();
        product.setId(7L);
        product.setBarcode("6001000000017");
        product.setName("Sugar 1kg");
        product.setBranchId(1L);
        product.setPrice(new BigDecimal("600.00"));
        product.setQuantityInStock(40);
        product.setLowStockThreshold(10);
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    @Test
    void barcodeLookup_cacheHit_returnsCachedProductWithoutQueryingMySQL() {
        when(cache.getProductByBarcode("6001000000017")).thenReturn(Optional.of(product));

        BarcodeLookupResponse response = productService.lookupByBarcode("6001000000017");

        assertThat(response.cacheHit()).isTrue();
        assertThat(response.product().name()).isEqualTo("Sugar 1kg");
        verify(productRepository, never()).findByBarcode(anyString());
    }

    @Test
    void barcodeLookup_cacheMiss_queriesMySQLAndStoresResultInRedis() {
        when(cache.getProductByBarcode("6001000000017")).thenReturn(Optional.empty());
        when(productRepository.findByBarcode("6001000000017")).thenReturn(Optional.of(product));

        BarcodeLookupResponse response = productService.lookupByBarcode("6001000000017");

        assertThat(response.cacheHit()).isFalse();
        assertThat(response.product().id()).isEqualTo(7L);
        verify(productRepository).findByBarcode("6001000000017");
        verify(cache).putProductByBarcode("6001000000017", product);
    }

    @Test
    void barcodeLookup_unknownBarcode_throwsNotFound() {
        when(cache.getProductByBarcode("nope")).thenReturn(Optional.empty());
        when(productRepository.findByBarcode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.lookupByBarcode("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(cache, never()).putProductByBarcode(anyString(), any());
    }

    @Test
    void update_priceChange_invalidatesProductCache() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest("6001000000017", "Sugar 1kg",
                1L, 1L, 1L, new BigDecimal("450.00"), new BigDecimal("650.00"), null, null, true);

        ProductDto dto = productService.update(7L, request);

        assertThat(dto.price()).isEqualByComparingTo("650.00");
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("650.00");
        // MySQL updated first, then the cache entry for this product is dropped.
        verify(cache).evictProduct(captor.getValue());
    }

    @Test
    void update_barcodeChanged_invalidatesOldAndNewBarcodeKeys() {
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductRequest request = new ProductRequest("6001000000999", "Sugar 1kg",
                1L, 1L, 1L, new BigDecimal("450.00"), new BigDecimal("600.00"), null, null, true);

        productService.update(7L, request);

        // evictProduct() drops product:id:{id} + product:barcode:{new barcode},
        // and the old barcode key is explicitly removed too.
        verify(cache).evictProduct(product);
        verify(cache).evictBarcode("6001000000017"); // old barcode
    }

    @Test
    void create_duplicateBarcode_rejected() {
        ProductRequest request = new ProductRequest("6001000000017", "Sugar 1kg",
                1L, 1L, 1L, null, new BigDecimal("600.00"), 10, 5, true);
        when(productRepository.existsByBarcode("6001000000017")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getById_usesRedisIdCacheOnSecondCall() {
        when(cache.getProductById(7L)).thenReturn(Optional.empty());
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        ProductDto first = productService.getByIdCached(7L);
        assertThat(first.id()).isEqualTo(7L);
        verify(cache).putProductById(7L, product);

        // Second call is answered from product:id:{id} - MySQL untouched.
        when(cache.getProductById(7L)).thenReturn(Optional.of(product));
        ProductDto second = productService.getByIdCached(7L);
        assertThat(second.name()).isEqualTo("Sugar 1kg");
        verify(productRepository).findById(7L); // exactly once, from the first call
    }

    @Test
    void benchmark_returnsMeasuredTimings() {
        when(productRepository.findByBarcode("6001000000017")).thenReturn(Optional.of(product));
        when(cache.getProductByBarcode("6001000000017")).thenReturn(Optional.of(product));

        BarcodeBenchmarkResponse result = productService.benchmark("6001000000017", 5);

        assertThat(result.iterations()).isEqualTo(5);
        assertThat(result.avgDbLookupMs()).isNotNegative();
        assertThat(result.avgRedisLookupMs()).isNotNegative();
        assertThat(result.note()).contains("Measured on this machine");
    }
}
