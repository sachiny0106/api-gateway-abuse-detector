package com.abuseguard.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("api-health", r -> r
                .path("/api/health")
                .filters(f -> f.stripPrefix(0))
                .uri("http://localhost:8080"))
            .route("api-data", r -> r
                .path("/api/data/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8080"))
            .route("api-login", r -> r
                .path("/api/login")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8080"))
            .route("api-users", r -> r
                .path("/api/users/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8080"))
            .route("api-export", r -> r
                .path("/api/export/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8080"))
            .route("api-download", r -> r
                .path("/api/download/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8080"))
            .route("api-change-email", r -> r
                .path("/api/change-email")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8080"))
            .route("api-change-password", r -> r
                .path("/api/change-password")
                .filters(f -> f.stripPrefix(1))
                .uri("http://localhost:8080"))
            .build();
    }
}