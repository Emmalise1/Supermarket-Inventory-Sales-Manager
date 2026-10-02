package com.supermarket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the complete Spring application (all beans: security, JPA, Mongo
 * repositories, RabbitMQ listener config, Redis cache, controllers) against
 * an in-memory H2 database.
 *
 * <p>MySQL/MongoDB/RabbitMQ/Redis are NOT required for this test: listener
 * startup and dynamic declaration are disabled, Mongo connects lazily, and
 * Redis failures are handled gracefully by the cache service - exactly the
 * fallback behaviour required of the application.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:supermarket-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false"
})
@AutoConfigureMockMvc
class ApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("token").asText()).isNotBlank();
        return body.get("token").asText();
    }

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }

    @Test
    void loginIssuesToken_andTokenUnlocksBarcodeLookup() throws Exception {
        String token = login("admin@supermarket.rw", "Admin@123");

        // Redis may be offline here: the lookup must still succeed via MySQL
        // and honestly report that it did not come from cache.
        mockMvc.perform(get("/api/products/barcode/6001000000017")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.barcode").value("6001000000017"))
                .andExpect(jsonPath("$.product.name").value("Sugar 1kg"));
    }

    @Test
    void wrongPassword_isRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@supermarket.rw\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_is401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cashier_cannotOpenAdminEndpoint() throws Exception {
        String token = login("cashier@supermarket.rw", "Cashier@123");

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboard_andProductList_workEndToEnd() throws Exception {
        String token = login("manager@supermarket.rw", "Manager@123");

        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
