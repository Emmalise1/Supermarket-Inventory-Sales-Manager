package com.supermarket.config;

import com.supermarket.security.GoogleOAuth2Handlers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Security for the two entry points of the application:
 *
 * <ol>
 *   <li>the OAuth2 (Google) sign-in chain - only while Google credentials are configured,</li>
 *   <li>the stateless OAuth2 resource-server chain used by the REST API.</li>
 * </ol>
 *
 * <ul>
 *   <li>Public: {@code POST /api/auth/login}, {@code GET /api/auth/providers}, health checks,
 *       CORS preflight.</li>
 *   <li>Everything else requires a valid JWT bearer token.</li>
 *   <li>RBAC roles come from the JWT {@code role} claim (ROLE_ADMIN / ROLE_MANAGER / ROLE_CASHIER)
 *       and are enforced with {@code @PreAuthorize} (method security).</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * OAuth2 (Google) sign-in chain - matched <em>before</em> the API chain so that adding
     * {@code oauth2Login()} can never change how the REST API answers unauthenticated calls
     * (they must keep returning 401, not a redirect to a login page).
     *
     * <p>It only claims the two OAuth2 endpoints, and only while Google credentials are
     * configured; otherwise those paths fall through to {@link #securityFilterChain} exactly
     * like any other unknown endpoint. Everything is read through {@code ObjectProvider} so
     * that no bean - not the client registration, not the handlers - is required when the
     * feature is switched off.</p>
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oauth2LoginFilterChain(HttpSecurity http,
                                                      ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                                      ObjectProvider<GoogleOAuth2Handlers> googleHandlers,
                                                      CorsConfigurationSource corsConfigurationSource) throws Exception {
        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        GoogleOAuth2Handlers handlers = googleHandlers.getIfAvailable();

        http
                .securityMatcher((HttpServletRequest request) ->
                        registrations != null && isOAuth2Endpoint(request))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // the OAuth2 dance stores the request in the session between redirect and callback
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        if (registrations != null && handlers != null) {
            http.oauth2Login(oauth2 -> oauth2
                    // under /api so the Vite dev-server proxy forwards it unchanged
                    .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/oauth2/authorization"))
                    .successHandler(handlers)
                    .failureHandler(handlers));
        }
        return http.build();
    }

    private static boolean isOAuth2Endpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/oauth2/authorization/") || uri.startsWith("/login/oauth2/code/");
    }

    /** Stateless OAuth2 resource-server security for the REST API. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/providers",
                                "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.sendError(401, "Invalid or missing access token");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.sendError(403, "Access denied");
                        }));
        return http.build();
    }

    /** Maps the JWT {@code role} claim to Spring authorities (ROLE_*). */
    /** BCrypt for storing password hashes - hashes are never cached or logged. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
