package com.supermarket.config;

import com.supermarket.domain.Branch;
import com.supermarket.domain.Category;
import com.supermarket.domain.Product;
import com.supermarket.domain.Role;
import com.supermarket.domain.Supplier;
import com.supermarket.domain.User;
import com.supermarket.repository.BranchRepository;
import com.supermarket.repository.CategoryRepository;
import com.supermarket.repository.ProductRepository;
import com.supermarket.repository.SupplierRepository;
import com.supermarket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Demo data for local development. Runs only when the tables are empty,
 * so it never overwrites real data. Disable with APP_SEED_ENABLED=false.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
                                      BranchRepository branchRepository,
                                      CategoryRepository categoryRepository,
                                      SupplierRepository supplierRepository,
                                      ProductRepository productRepository,
                                      PasswordEncoder passwordEncoder,
                                      @org.springframework.beans.factory.annotation.Value("${app.seed.enabled:true}")
                                              boolean seedEnabled) {
        return args -> {
            if (!seedEnabled || userRepository.count() > 0) {
                return;
            }

            Branch kigali = branchRepository.save(new Branch("Kigali Main", "KN 4 Ave, Kigali", "+250 788 000 001"));
            Branch butare = branchRepository.save(new Branch("Huye Branch", "Rwanda Ave, Huye", "+250 788 000 002"));

            userRepository.save(new User("admin@supermarket.rw",
                    passwordEncoder.encode("Admin@123"), "System Admin", Role.ADMIN, null));
            userRepository.save(new User("manager@supermarket.rw",
                    passwordEncoder.encode("Manager@123"), "Store Manager", Role.MANAGER, kigali.getId()));
            userRepository.save(new User("cashier@supermarket.rw",
                    passwordEncoder.encode("Cashier@123"), "POS Cashier", Role.CASHIER, kigali.getId()));

            Category groceries = categoryRepository.save(new Category("Groceries"));
            Category beverages = categoryRepository.save(new Category("Beverages"));
            Category household = categoryRepository.save(new Category("Household"));

            Supplier atlas = supplierRepository.save(new Supplier("Atlas Wholesale", "Jean Bizimana", "+250 788 111 222", "sales@atlas.rw"));
            Supplier kivu = supplierRepository.save(new Supplier("Kivu Foods", "Aline Uwase", "+250 788 333 444", "orders@kivufoods.rw"));

            saveProduct(productRepository, "6001000000017", "Sugar 1kg", groceries, atlas, kigali, "450", "600", 40, 10);
            saveProduct(productRepository, "6001000000024", "Rice 5kg", groceries, atlas, kigali, "3500", "4500", 25, 5);
            saveProduct(productRepository, "6001000000031", "Cooking Oil 2L", groceries, kivu, kigali, "3800", "4800", 18, 6);
            saveProduct(productRepository, "6001000000048", "Milk 1L", beverages, kivu, kigali, "900", "1200", 30, 12);
            saveProduct(productRepository, "6001000000055", "Soda 500ml", beverages, atlas, kigali, "500", "800", 60, 20);
            saveProduct(productRepository, "6001000000062", "Bottled Water 1.5L", beverages, atlas, kigali, "400", "700", 8, 15); // low stock
            saveProduct(productRepository, "6001000000079", "Soap Bar 250g", household, kivu, kigali, "700", "1000", 35, 10);
            saveProduct(productRepository, "6001000000086", "Detergent 1kg", household, kivu, kigali, "2200", "3000", 14, 5);
            saveProduct(productRepository, "6001000000093", "Sugar 1kg (Huye)", groceries, atlas, butare, "450", "600", 22, 10);
            saveProduct(productRepository, "6001000000109", "Rice 5kg (Huye)", groceries, atlas, butare, "3500", "4500", 12, 5);

            log.info("Seeded demo data.");
            log.info("Login -> admin@supermarket.rw / Admin@123 (ADMIN)");
            log.info("       -> manager@supermarket.rw / Manager@123 (MANAGER, Kigali)");
            log.info("       -> cashier@supermarket.rw / Cashier@123 (CASHIER, Kigali)");
        };
    }

    private static void saveProduct(ProductRepository repo, String barcode, String name,
                                    Category category, Supplier supplier, Branch branch,
                                    String cost, String price, int stock, int threshold) {
        Product product = new Product();
        product.setBarcode(barcode);
        product.setName(name);
        product.setCategoryId(category.getId());
        product.setSupplierId(supplier.getId());
        product.setBranchId(branch.getId());
        product.setCostPrice(new BigDecimal(cost));
        product.setPrice(new BigDecimal(price));
        product.setQuantityInStock(stock);
        product.setLowStockThreshold(threshold);
        repo.save(product);
    }
}
