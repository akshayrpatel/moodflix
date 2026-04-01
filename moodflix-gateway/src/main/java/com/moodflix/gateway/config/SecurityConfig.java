package com.moodflix.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the MoodFlix gateway.
 *
 * <p>Configures a stateless, JWT-based security model appropriate
 * for an API gateway. Three Spring Security defaults are explicitly
 * disabled for the following reasons:</p>
 *
 * <ul>
 *   <li><b>CSRF</b> — disabled because authentication is JWT-based,
 *       not cookie-based. CSRF attacks require a session cookie to
 *       exploit — stateless JWT auth has no such surface.</li>
 *   <li><b>HTTP Basic</b> — disabled to prevent credentials from
 *       being transmitted in plain headers. All auth flows through
 *       {@code /auth/**} endpoints explicitly.</li>
 *   <li><b>Form Login</b> — disabled because this is a REST API
 *       gateway, not a browser-facing application. No HTML login
 *       form should ever be served.</li>
 * </ul>
 *
 * <p>Public routes ({@code /auth/**}) are permitted without
 * authentication. All other routes require a valid JWT, enforced
 * downstream by {@code JwtAuthFilter}.</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the reactive security filter chain.
     *
     * @param http the reactive HTTP security configurer
     * @return the configured {@link SecurityWebFilterChain}
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers("/api/search/**").permitAll()
                        .pathMatchers("/api/discover").permitAll()
                        .pathMatchers("/onboarding/**").permitAll()
                        .pathMatchers("/discovery/**").permitAll()
                        .pathMatchers("/profile/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }

    /**
     * Provides a BCrypt password encoder for hashing user passwords
     * at registration time.
     *
     * <p>BCrypt is chosen because it is adaptive — the work factor
     * can be increased as hardware improves without invalidating
     * existing hashes.</p>
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    /**
     * CORS configuration allowing the frontend origin.
     *
     * <p>Permits all standard headers and methods from
     * {@code localhost:3000} so the browser does not block
     * auth and API requests.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}