package com.neighborhood.eventmanagement.config;

import com.neighborhood.eventmanagement.security.service.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class SecurityBeansConfig {


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }



    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder) {


        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();


        provider.setUserDetailsService(
                customUserDetailsService
        );


        provider.setPasswordEncoder(
                passwordEncoder
        );


        return provider;
    }

}