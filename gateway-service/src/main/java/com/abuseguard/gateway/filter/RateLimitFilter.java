package com.abuseguard.gateway.filter;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.gateway.metrics.GatewayMetrics;
import com.abuseguard.gateway.ratelimit.SlidingWindowRateLimiter;
import com.abuseguard.gateway.ratelimit.RateLimitResult;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int DEFAULT_RATE_LIMIT = 100;
    private static final int DEFAULT_WINDOW_SECONDS = 60;

    private final SlidingWindowRateLimiter rateLimiter;
    private final GatewayMetrics metrics;

    public RateLimitFilter(SlidingWindowRateLimiter rateLimiter, GatewayMetrics metrics) {
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        RequestEvent event = MetadataExtractorFilter.getRequestEvent(exchange);
        if (event == null) {
            return chain.filter(exchange);
        }

        String apiKey = event.apiKey();
        String ip = event.ip();

        Mono<RateLimitResult> userLimitMono = apiKey != null && !apiKey.isEmpty()
            ? rateLimiter.isAllowedPerUser(apiKey, DEFAULT_WINDOW_SECONDS, DEFAULT_RATE_LIMIT)
            : Mono.just(new RateLimitResult(true, 0, DEFAULT_RATE_LIMIT, DEFAULT_RATE_LIMIT));

        Mono<RateLimitResult> ipLimitMono = rateLimiter.isAllowedPerIp(ip, DEFAULT_WINDOW_SECONDS, DEFAULT_RATE_LIMIT * 2);

        return Mono.zip(userLimitMono, ipLimitMono)
            .flatMap(tuple -> {
                RateLimitResult userResult = tuple.getT1();
                RateLimitResult ipResult = tuple.getT2();

                if (!userResult.allowed() || !ipResult.allowed()) {
                    metrics.incrementRateLimited();
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(DEFAULT_RATE_LIMIT));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                    exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(DEFAULT_WINDOW_SECONDS));
                    return exchange.getResponse().setComplete();
                }

                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(DEFAULT_RATE_LIMIT));
                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(Math.min(userResult.remaining(), ipResult.remaining())));
                exchange.getResponse().getHeaders().add("X-RateLimit-Reset", String.valueOf(DEFAULT_WINDOW_SECONDS));

                return chain.filter(exchange);
            });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}