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

                // Public — auth + Swagger + docs
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // ADMIN only
                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                // COMMUNITY_MANAGER + ADMIN — event approval/management
                .requestMatchers("/api/events/manage/**")
                    .hasAnyRole("COMMUNITY_MANAGER", "ADMIN")

                // ZONE_COORDINATOR + COMMUNITY_MANAGER + ADMIN — zone management
                .requestMatchers("/api/zones/manage/**")
                    .hasAnyRole("ZONE_COORDINATOR", "COMMUNITY_MANAGER", "ADMIN")

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
