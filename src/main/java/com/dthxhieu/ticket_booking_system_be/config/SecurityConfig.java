package com.dthxhieu.ticket_booking_system_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Disable CSRF — this is a stateless JWT API, no session cookies.
                .csrf(AbstractHttpConfigurer::disable)

                // Use stateless session — Spring Security must not create HttpSession.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints: register, verify-otp, login, refresh, forgot/reset password
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Public category reads - BR-07: read operations may be public
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // Public venue reads - BR-07 (US-08)
                        .requestMatchers(HttpMethod.GET, "/api/v1/venues/**").permitAll()

                        // Public seat reads — GET /api/v1/seats/{id} and
                        // GET /api/v1/venues/{venueId}/seats (BR-12, US-09)
                        .requestMatchers(HttpMethod.GET, "/api/v1/seats/**").permitAll()

                        // Public event reads — GET /api/v1/events and GET /api/v1/events/{id} (US-10)
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()

                        // Public event session reads — GET /api/v1/event-sessions (US-11)
                        .requestMatchers(HttpMethod.GET, "/api/v1/event-sessions/**").permitAll()

                        // Admin-only write operations on categories - BR-06
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // PayOS webhook — must be public (no JWT). PayOS calls this from their servers.
                        // Signature verification is performed inside PaymentWebhookService.
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook/payos").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
