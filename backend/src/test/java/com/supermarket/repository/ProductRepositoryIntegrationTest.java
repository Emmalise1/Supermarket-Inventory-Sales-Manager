package com.supermarket.repository;

import com.supermarket.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository integration test against an in-memory database (H2) -
 * the application's MySQL configuration is untouched for production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product(String barcode, String name, int stock, int threshold) {
        Product product = new Product();
        product.setBarcode(barcode);
        product.setName(name);
        product.setBranchId(1L);
        product.setPrice(new BigDecimal("1000.00"));
        product.setQuantityInStock(stock);
        product.setLowStockThreshold(threshold);
        return productRepository.save(product);
    }

    @Test
    void findByBarcode_returnsSavedProduct() {
        product("6001000000001", "Rice 1kg", 20, 5);

        Optional<Product> found = productRepository.findByBarcode("6001000000001");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Rice 1kg");
    }

    @Test
    void duplicateBarcode_isRejectedByUniqueConstraint() {
        product("6001000000002", "First", 10, 5);

        assertThatThrownBy(() -> product("6001000000002", "Second", 10, 5))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lowStockQuery_returnsOnlyProductsAtOrBelowThreshold() {
        product("6001000000003", "Low", 3, 10);
        product("6001000000004", "Healthy", 50, 10);

        List<Product> lowStock = productRepository.findLowStock();

        assertThat(lowStock).extracting(Product::getName).containsExactly("Low");
    }

    @Test
    void deactivationRemovesProductFromLowStockResults() {
        Product low = product("6001000000005", "To deactivate", 1, 10);
        low.setActive(false);
        productRepository.saveAndFlush(low);

        assertThat(productRepository.findLowStock()).isEmpty();
    }
}
