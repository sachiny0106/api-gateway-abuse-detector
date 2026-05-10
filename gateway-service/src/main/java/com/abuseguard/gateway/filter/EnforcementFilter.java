package com.abuseguard.gateway.filter;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.common.model.ActionType;
import com.abuseguard.common.model.Decision;
import com.abuseguard.gateway.decision.DecisionStore;
import com.abuseguard.gateway.metrics.GatewayMetrics;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class EnforcementFilter implements GlobalFilter, Ordered {

    private final DecisionStore decisionStore;
    private final GatewayMetrics metrics;

    public EnforcementFilter(DecisionStore decisionStore, GatewayMetrics metrics) {
        this.decisionStore = decisionStore;
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

        Mono<Decision> decisionMono;
        if (apiKey != null && !apiKey.isEmpty()) {
            decisionMono = decisionStore.getDecision(apiKey);
        } else {
            decisionMono = decisionStore.getDecisionByIp(ip);
        }

        return decisionMono.flatMap(decision -> {
            if (decision == null || decision.action() == ActionType.ALLOW) {
                return chain.filter(exchange);
            }

            switch (decision.action()) {
                case BLOCK -> {
                    metrics.incrementBlocked();
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    exchange.getResponse().getHeaders().add("X-Blocked-Reason", decision.reason());
                    return exchange.getResponse().setComplete();
                }
                case THROTTLE -> {
                    metrics.incrementRateLimited();
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().add("X-RateLimit-Retry-After", "15");
                    return exchange.getResponse().setComplete();
                }
                case FLAG -> {
                    exchange.getResponse().getHeaders().add("X-Flagged", "true");
                    metrics.incrementFlagged();
                    return chain.filter(exchange);
                }
                default -> {
                    return chain.filter(exchange);
                }
            }
        })
        .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)))
        .onErrorResume(e -> chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}