package com.supermarket.web;

import com.supermarket.config.JwtConfig;
import com.supermarket.config.SecurityConfig;
import com.supermarket.domain.Role;
import com.supermarket.dto.ProductDtos.ProductDto;
import com.supermarket.dto.ProductDtos.ProductRequest;
import com.supermarket.security.JwtService;
import com.supermarket.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security/RBAC tests over a real Spring Security filter chain:
 * anonymous -&gt; 401, wrong role -&gt; 403, correct role -&gt; 200.
 */
@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfig.class, JwtConfig.class, JwtService.class})
class ProductApiSecurityTest {

    private static final String CREATE_BODY = """
            {
              "barcode": "6001000000777",
              "name": "Test Product",
              "branchId": 1,
              "price": 1000.00
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ProductService productService;

    private String tokenFor(Role role, Long branchId) {
        return jwtService.issueToken(1L, "user@supermarket.rw", "Test User", role, branchId);
    }

    @Test
    void anonymousRequest_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/products/barcode/6001000000017"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgedToken_isRejectedWith401() throws Exception {
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.forgedsignature";
        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cashierCannotCreateProduct_is403() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + tokenFor(Role.CASHIER, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanCreateProduct_is200() throws Exception {
        when(productService.create(any(ProductRequest.class)))
                .thenReturn(new ProductDto(1L, "6001000000777", "Test Product",
                        null, null, 1L, null, new BigDecimal("1000.00"),
                        0, 10, true, null));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + tokenFor(Role.MANAGER, 1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("6001000000777"));
    }

    @Test
    void cashierCanLookUpBarcode_is200() throws Exception {
        when(productService.lookupByBarcode("6001000000017"))
                .thenReturn(new com.supermarket.dto.ProductDtos.BarcodeLookupResponse(
                        new ProductDto(1L, "6001000000017", "Sugar 1kg", null, null, 1L,
                                null, new BigDecimal("600.00"), 40, 10, true, null),
                        true, 1L));

        mockMvc.perform(get("/api/products/barcode/6001000000017")
                        .header("Authorization", "Bearer " + tokenFor(Role.CASHIER, 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheHit").value(true));
    }

    @Test
    void benchmarkEndpoint_forbiddenForCashier() throws Exception {
        mockMvc.perform(get("/api/products/barcode/6001000000017/benchmark")
                        .header("Authorization", "Bearer " + tokenFor(Role.CASHIER, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unvalidatedBody_is400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + tokenFor(Role.ADMIN, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing barcode and price\"}"))
                .andExpect(status().isBadRequest());
    }
}
