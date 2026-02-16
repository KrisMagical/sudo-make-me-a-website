package com.magiccode.backend.config;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String[] DOCS = {
            "/login", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    public static final String[] PUBLIC_GET = {
            "/api/images/**",
            "/api/home/**",
            "/api/categories/**",
            "/api/posts/**",
            "/api/pages/**",
            "/api/socials",
            "/api/videos/**",
            "/api/comments/**",
            "/api/sidebar/**"
    };

    public static final String[] PUBLIC_POST = {
            "/api/comments/**",
            "/api/posts/*/like"
    };
}
