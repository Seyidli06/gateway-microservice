package com.adil.profileservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Profile Service API",
                version = "1.0.0",
                description = """
                        REST API for managing user profiles.

                        Supports profile creation, retrieval, update,
                        deletion, pagination and sorting.
                        """,
                contact = @Contact(
                        name = "Gateway Microservices Team"
                ),
                license = @License(
                        name = "MIT"
                )
        )
)
public class OpenApiConfig {
}