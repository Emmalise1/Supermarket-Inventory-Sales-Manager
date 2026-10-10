package com.supermarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the application exactly like production but with Google OAuth2 credentials set,
 * and proves three things:
 *
 * <ol>
 *   <li>the login page is told that Google sign-in is available,</li>
 *   <li>the authorization endpoint really starts the OAuth2 authorization code flow
 *       (302 to Google with our client id and redirect URI),</li>
 *   <li>adding {@code oauth2Login()} did NOT change how the REST API answers callers
 *       without a token - it must stay 401 with no {@code Location} header, never a
 *       redirect to a login page.</li>
 * </ol>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:supermarket-oauth2-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false",
        "app.security.google.client-id=123456.apps.googleusercontent.com",
        "app.security.google.client-secret=test-client-secret",
        "app.security.google.redirect-uri=http://localhost:8080/login/oauth2/code/google"
})
@AutoConfigureMockMvc
class GoogleOAuth2LoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providersEndpoint_reportsGoogleIsEnabled() throws Exception {
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value(true))
                .andExpect(jsonPath("$.google").value(true));
    }

    @Test
    void authorizationEndpoint_startsTheAuthorizationCodeFlowAtGoogle() throws Exception {
        String location = mockMvc.perform(get("/api/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getHeader("Location");

        assertThat(location).isNotNull();
        assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(location).contains("client_id=123456.apps.googleusercontent.com");
        assertThat(location).contains("response_type=code");
        assertThat(location).contains("state=");
        assertThat(location).satisfiesAnyOf(
                value -> assertThat(value).contains("login/oauth2/code/google"),
                value -> assertThat(value).contains("login%2Foauth2%2Fcode%2Fgoogle"));
    }

    @Test
    void apiWithoutToken_stillAnswers401_notALoginRedirect() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }
}
