package com.abuseguard.gateway.filter;

import com.abuseguard.common.events.RequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.abuseguard.gateway.metrics.GatewayMetrics;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class EventPublisherFilter implements GlobalFilter, Ordered {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final GatewayMetrics metrics;
    private static final String TOPIC = "request-events";

    public EventPublisherFilter(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, GatewayMetrics metrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        RequestEvent event = MetadataExtractorFilter.getRequestEvent(exchange);
        if (event == null) {
            return chain.filter(exchange);
        }

        String key = event.apiKey() != null ? event.apiKey() : event.ip();
        String json = serializeEvent(event);

        return chain.filter(exchange)
            .then(Mono.fromRunnable(() ->
                kafkaTemplate.send(TOPIC, key, json).whenComplete((result, ex) -> {
                    if (ex != null) {
                        metrics.incrementKafkaDropped();
                    } else {
                        metrics.incrementRequests();
                    }
                })
            ));
    }

    private String serializeEvent(RequestEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}