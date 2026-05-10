package com.abuseguard.gateway.decision;

import com.abuseguard.common.model.ActionType;
import com.abuseguard.common.model.Decision;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class DecisionStore {

    private final ReactiveStringRedisTemplate redisTemplate;

    public DecisionStore(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Decision> getDecision(String apiKey) {
        return redisTemplate.opsForHash().entries("decision:" + apiKey)
            .collectMap(
                entry -> entry.getKey().toString(),
                entry -> entry.getValue().toString()
            )
            .filter(map -> !map.isEmpty())
            .map(this::parseDecision);
    }

    public Mono<Decision> getDecisionByIp(String ip) {
        return redisTemplate.opsForHash().entries("decision:ip:" + ip)
            .collectMap(
                entry -> entry.getKey().toString(),
                entry -> entry.getValue().toString()
            )
            .filter(map -> !map.isEmpty())
            .map(this::parseDecision);
    }

    public Mono<Boolean> setDecision(String apiKey, Decision decision) {
        return setDecisionInternal("decision:" + apiKey, apiKey, decision);
    }

    public Mono<Boolean> setDecisionByIp(String ip, Decision decision) {
        return setDecisionInternal("decision:ip:" + ip, ip, decision);
    }

    private Mono<Boolean> setDecisionInternal(String key, String lookupKey, Decision decision) {
        Duration ttl = getTtlForAction(decision.action());

        Map<String, String> entries = Map.of(
            "action", decision.action().name(),
            "score", String.valueOf(decision.score()),
            "reason", decision.reason() != null ? decision.reason() : "",
            "expiresAt", decision.expiresAt().toString()
        );

        return redisTemplate.opsForHash().putAll(key, entries)
            .flatMap(success -> redisTemplate.expire(key, ttl))
            .thenReturn(true)
            .onErrorReturn(false);
    }

    private Decision parseDecision(Map<String, String> map) {
        ActionType action = ActionType.valueOf(map.getOrDefault("action", "ALLOW"));
        double score = Double.parseDouble(map.getOrDefault("score", "0.0"));
        String reason = map.getOrDefault("reason", "");
        Instant expiresAt = Instant.parse(map.getOrDefault("expiresAt", Instant.now().toString()));

        return new Decision(action, score, reason, expiresAt);
    }

    private Duration getTtlForAction(ActionType action) {
        return switch (action) {
            case ALLOW -> Duration.ofMinutes(1);
            case FLAG -> Duration.ofMinutes(5);
            case THROTTLE -> Duration.ofMinutes(15);
            case BLOCK -> Duration.ofHours(1);
        };
    }
}