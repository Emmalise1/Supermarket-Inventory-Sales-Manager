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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:supermarket-trend-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false"
})
@AutoConfigureMockMvc
class ReportTrendTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    @Test
    void emptyDatabase_returnsZeroFilledDays() throws Exception {
        String token = login("admin@supermarket.rw", "Admin@123");
        int days = 14;

        mockMvc.perform(get("/api/reports/trend?days=" + days)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(days))
                .andExpect(jsonPath("$[0].revenue").value(0))
                .andExpect(jsonPath("$[0].salesCount").value(0))
                .andExpect(jsonPath("$[" + (days - 1) + "].revenue").value(0))
                .andExpect(jsonPath("$[" + (days - 1) + "].salesCount").value(0));

        MvcResult result = mockMvc.perform(get("/api/reports/trend?days=" + days)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode points = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(points.isArray()).isTrue();
        assertThat(points.size()).isEqualTo(days);

        LocalDate previous = null;
        for (JsonNode point : points) {
            LocalDate date = LocalDate.parse(point.get("date").asText());
            if (previous != null) {
                assertThat(date).isEqualTo(previous.plusDays(1));
            }
            previous = date;
        }
        assertThat(previous).isEqualTo(LocalDate.now());
    }
}
