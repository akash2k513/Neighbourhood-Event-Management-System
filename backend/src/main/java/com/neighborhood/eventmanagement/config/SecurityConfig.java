package com.neighborhood.eventmanagement.config;

import com.neighborhood.eventmanagement.security.jwt.JwtAuthenticationEntryPoint;
import com.neighborhood.eventmanagement.security.jwt.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            AuthenticationProvider authenticationProvider) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth

                // Public — auth + Swagger + docs + read-only browsing
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/api/zones",
                    "/api/zones/{id}",
                    "/api/zones/{id}/events",
                    "/api/zones/{id}/residents",
                    "/api/venues",
                    "/api/venues/{id}",
                    "/api/venues/available",
                    "/api/venues/zone/{zoneId}",
                    "/api/resources",
                    "/api/resources/{id}",
                    "/api/resources/available",
                    "/api/events",
                    "/api/events/{id}",
                    "/api/events/upcoming",
                    "/api/events/search",
                    "/api/events/category/**",
                    "/api/events/calendar",
                    "/api/events/{id}/feedback"
                ).permitAll()

                // ADMIN only
                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                // COMMUNITY_MANAGER + ADMIN — event approval/management
                .requestMatchers("/api/events/manage/**", "/api/approvals/**")
                    .hasAnyRole("COMMUNITY_MANAGER", "ADMIN")

                // ZONE_COORDINATOR + COMMUNITY_MANAGER + ADMIN — zone management
                .requestMatchers("/api/zones/manage/**")
                    .hasAnyRole("ZONE_COORDINATOR", "COMMUNITY_MANAGER", "ADMIN")

                // ZONE_COORDINATOR + COMMUNITY_MANAGER + ADMIN — venue write
                .requestMatchers(org.springframework.http.HttpMethod.POST,   "/api/venues")
                    .hasAnyRole("ZONE_COORDINATOR", "COMMUNITY_MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/api/venues/{id}")
                    .hasAnyRole("ZONE_COORDINATOR", "COMMUNITY_MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/venues/{id}")
                    .hasRole("ADMIN")

                // COMMUNITY_MANAGER + ADMIN — resource create/update/delete
                .requestMatchers(org.springframework.http.HttpMethod.POST,   "/api/resources")
                    .hasAnyRole("COMMUNITY_MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/api/resources/{id}")
                    .hasAnyRole("COMMUNITY_MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/resources/{id}")
                    .hasRole("ADMIN")

                // COMMUNITY_MANAGER + ADMIN — send/broadcast notifications
                .requestMatchers("/api/notifications/send", "/api/notifications/broadcast")
                    .hasAnyRole("COMMUNITY_MANAGER", "ADMIN")

                // COMMUNITY_MANAGER + ADMIN — analytics and reports
                .requestMatchers("/api/analytics/**", "/api/reports/**")
                    .hasAnyRole("COMMUNITY_MANAGER", "ADMIN")

                // EVENT_ORGANIZER + COMMUNITY_MANAGER + ADMIN — organizer actions
                .requestMatchers("/api/events/organizer/**")
                    .hasAnyRole("EVENT_ORGANIZER", "COMMUNITY_MANAGER", "ADMIN")

                // RESIDENT + EVENT_ORGANIZER + above — resident actions
                .requestMatchers("/api/resident/**")
                    .hasAnyRole("RESIDENT", "EVENT_ORGANIZER", "ZONE_COORDINATOR",
                                "COMMUNITY_MANAGER", "ADMIN")

                // GUEST — public read-only browsing
                .requestMatchers("/api/guest/**")
                    .hasAnyRole("GUEST", "RESIDENT", "EVENT_ORGANIZER", "ZONE_COORDINATOR",
                                "COMMUNITY_MANAGER", "ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
