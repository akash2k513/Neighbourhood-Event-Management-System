package com.neighborhood.eventmanagement.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI eventManagementOpenAPI() {

        return new OpenAPI()

                .info(new Info()

                        .title("Neighborhood Event Management API")

                        .description("""
                                REST API documentation for the
                                Neighborhood Event Management System.
                                
                                Features:
                                • User Management
                                • Event Management
                                • Registration
                                • Authentication
                                • Notifications
                                • Audit Logs
                                """)

                        .version("1.0")

                        .contact(new Contact()
                                .name("Neighborhood Event Management Team")
                                .email("support@example.com"))

                        .license(new License()
                                .name("MIT License")))

                .externalDocs(new ExternalDocumentation()
                        .description("Project Documentation")
                        .url("https://github.com/"));
    }
}