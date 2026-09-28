package com.magiccode.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI blogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("sudo-make-me-a-website API")
                        .description("Personal blog API for public posts, comments, search, social links, admin management, authentication, media, and maintenance mode.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Admin endpoints require Authorization: Bearer <token>.")))
                .tags(List.of(
                        new Tag().name("Public Posts").description("Public article reading, search, and reactions."),
                        new Tag().name("Public Comments").description("Visitor comment submission and approved comment listing."),
                        new Tag().name("Public Search").description("Public post and collection search."),
                        new Tag().name("Public Social").description("Public social link and sidebar data."),
                        new Tag().name("Admin Auth").description("Admin login and token validation."),
                        new Tag().name("Admin Posts").description("Authenticated article, category, and collection management."),
                        new Tag().name("Admin Comments").description("Authenticated comment moderation."),
                        new Tag().name("Admin Media").description("Authenticated media upload and deletion."),
                        new Tag().name("Admin Maintenance").description("Authenticated maintenance mode management."),
                        new Tag().name("Admin Config").description("Authenticated site, sidebar, and browser icon configuration.")
                ));
    }
}
