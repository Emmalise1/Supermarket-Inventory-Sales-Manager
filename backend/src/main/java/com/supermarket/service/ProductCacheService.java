package com.supermarket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis caching layer.
 *
 * <p>Redis is a <b>cache only</b> - MySQL remains the source of truth.
 * Every operation is wrapped so that a Redis outage degrades gracefully:
 * the application simply falls back to MySQL.</p>
 *
 * <p>Key naming convention:</p>
 * <ul>
 *   <li>{@code product:barcode:{barcode}} - product by barcode</li>
 *   <li>{@code product:id:{id}} - product by id</li>
 *   <li>{@code dashboard:branch:{branchId}} - dashboard summary</li>
 * </ul>
 *
 * <p>Never stores credentials, passwords or tokens.</p>
 */
@Service
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration productTtl;
    private final Duration dashboardTtl;

    /** Used to log Redis outages once instead of on every call. */
    private volatile boolean redisDownLogged = false;

    public ProductCacheService(StringRedisTemplate redis,
                               ObjectMapper objectMapper,
                               @Value("${app.cache.product-ttl-minutes:10}") long productTtlMinutes,
                               @Value("${app.cache.dashboard-ttl-minutes:5}") long dashboardTtlMinutes) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.productTtl = Duration.ofMinutes(productTtlMinutes);
        this.dashboardTtl = Duration.ofMinutes(dashboardTtlMinutes);
    }

    public static String barcodeKey(String barcode) {
        return "product:barcode:" + barcode;
    }

    public static String productKey(Long id) {
        return "product:id:" + id;
    }

    public static String dashboardKey(Long branchId) {
        return "dashboard:branch:" + (branchId == null ? "all" : branchId);
    }

    // ------------------------------------------------------------------ read

    public Optional<Product> getProductByBarcode(String barcode) {
        return getProduct(barcodeKey(barcode));
    }

    public Optional<Product> getProductById(Long id) {
        return getProduct(productKey(id));
    }

    public Optional<String> getJson(String key) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key));
        } catch (RuntimeException ex) {
            handleRedisFailure("GET " + key, ex);
            return Optional.empty();
        }
    }

    private Optional<Product> getProduct(String key) {
        Optional<String> json = getJson(key);
        if (json.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json.get(), Product.class));
        } catch (JsonProcessingException ex) {
            // Corrupted entry: drop it and let MySQL answer next time.
            evictKey(key);
            return Optional.empty();
        }
    }

    // ----------------------------------------------------------------- write

    public void putProductByBarcode(String barcode, Product product) {
        putJson(barcodeKey(barcode), toJson(product), productTtl);
    }

    public void putProductById(Long id, Product product) {
        putJson(productKey(id), toJson(product), productTtl);
    }

    public void putJson(String key, String json, Duration ttl) {
        if (json == null) {
            return;
        }
        try {
            redis.opsForValue().set(key, json, ttl);
        } catch (RuntimeException ex) {
            handleRedisFailure("SET " + key, ex);
        }
    }

    public Duration getDashboardTtl() {
        return dashboardTtl;
    }

    // ----------------------------------------------------------- invalidation

    /** Invalidates both the id and barcode entries of a product. */
    public void evictProduct(Product product) {
        if (product == null) {
            return;
        }
        evictKey(productKey(product.getId()));
        if (product.getBarcode() != null) {
            evictKey(barcodeKey(product.getBarcode()));
        }
    }

    public void evictBarcode(String barcode) {
        if (barcode != null) {
            evictKey(barcodeKey(barcode));
        }
    }

    public void evictKey(String key) {
        try {
            redis.delete(key);
        } catch (RuntimeException ex) {
            handleRedisFailure("DEL " + key, ex);
        }
    }

    // -------------------------------------------------------------- internals

    private String toJson(Product product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialize product {} for caching", product.getId(), ex);
            return null;
        }
    }

    private void handleRedisFailure(String operation, RuntimeException ex) {
        if (!redisDownLogged) {
            redisDownLogged = true;
            log.warn("Redis unavailable ({}). Falling back to MySQL: {}", operation, ex.getMessage());
        }
    }
}
