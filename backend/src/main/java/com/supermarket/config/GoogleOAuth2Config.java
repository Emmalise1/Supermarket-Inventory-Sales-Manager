package com.supermarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Google OAuth2 client registration (authorization code flow).
 *
 * <p>The bean only exists when {@code app.security.google.client-id} is not empty, so the
 * application boots - and every existing test still passes - without any Google credentials.
 * When credentials are present, {@code GET /api/oauth2/authorization/google} starts the flow
 * and {@code GET /login/oauth2/code/google} receives Google's callback.</p>
 *
 * <p>Every endpoint is declared explicitly instead of relying on OpenID discovery, so starting
 * the application never needs network access.</p>
 */
@Configuration
@ConditionalOnExpression("'${app.security.google.client-id:}' != ''")
public class GoogleOAuth2Config {

    public static final String REGISTRATION_ID = "google";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.security.google.client-id}") String clientId,
            @Value("${app.security.google.client-secret}") String clientSecret,
            @Value("${app.security.google.redirect-uri:http://localhost:8080/login/oauth2/code/google}")
            String redirectUri) {

        ClientRegistration google = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientName("Google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userNameAttributeName("sub")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
