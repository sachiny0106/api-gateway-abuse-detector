package com.abuseguard.gateway.filter;

import com.abuseguard.common.events.RequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class MetadataExtractorFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_EVENT_ATTR = "requestEvent";
    private final ObjectMapper objectMapper;

    public MetadataExtractorFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String ip = extractClientIp(request);
        String apiKey = request.getHeaders().getFirst("X-API-Key");
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String contentType = request.getHeaders().getContentType() != null
            ? request.getHeaders().getContentType().toString()
            : null;
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        RequestEvent event = RequestEvent.builder()
            .timestamp(Instant.now())
            .ip(ip)
            .apiKey(apiKey)
            .endpoint(path)
            .method(method)
            .userAgent(userAgent)
            .contentType(contentType)
            .forwardedFor(forwardedFor)
            .build();

        exchange.getAttributes().put(REQUEST_EVENT_ATTR, event);

        return chain.filter(exchange);
    }

    private String extractClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    public static RequestEvent getRequestEvent(ServerWebExchange exchange) {
        return exchange.getAttribute(REQUEST_EVENT_ATTR);
    }

    @Override
    public int getOrder() {
        return -3;
    }
}