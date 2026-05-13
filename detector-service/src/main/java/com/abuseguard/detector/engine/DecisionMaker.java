package com.abuseguard.detector.engine;

import com.abuseguard.common.model.ActionType;
import com.abuseguard.common.model.Decision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class DecisionMaker {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Double> weights;
    private final double flagThreshold;
    private final double throttleThreshold;
    private final double blockThreshold;
    private final double overrideBlockThreshold;
    private final Duration flagTtl;
    private final Duration throttleTtl;
    private final Duration blockTtl;

    public DecisionMaker(
            StringRedisTemplate redisTemplate,
            MeterRegistry meterRegistry,
            @Value("${abuse-detector.weights.spike:0.25}") double spikeWeight,
            @Value("${abuse-detector.weights.brute-force:0.35}") double bruteForceWeight,
            @Value("${abuse-detector.weights.bot-pattern:0.20}") double botPatternWeight,
            @Value("${abuse-detector.weights.user-agent:0.10}") double userAgentWeight,
            @Value("${abuse-detector.weights.ip-behavior:0.15}") double ipBehaviorWeight,
            @Value("${abuse-detector.weights.sequence:0.30}") double sequenceWeight,
            @Value("${abuse-detector.thresholds.flag:0.45}") double flagThreshold,
            @Value("${abuse-detector.thresholds.throttle:0.65}") double throttleThreshold,
            @Value("${abuse-detector.thresholds.block:0.80}") double blockThreshold,
            @Value("${abuse-detector.thresholds.override-block:0.90}") double overrideBlockThreshold,
            @Value("${abuse-detector.ttl.flag-minutes:5}") int flagMinutes,
            @Value("${abuse-detector.ttl.throttle-minutes:15}") int throttleMinutes,
            @Value("${abuse-detector.ttl.block-minutes:60}") int blockMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.weights = Map.of(
            "spike", spikeWeight,
            "brute-force", bruteForceWeight,
            "bot-pattern", botPatternWeight,
            "user-agent", userAgentWeight,
            "ip-behavior", ipBehaviorWeight,
            "sequence", sequenceWeight
        );
        this.flagThreshold = flagThreshold;
        this.throttleThreshold = throttleThreshold;
        this.blockThreshold = blockThreshold;
        this.overrideBlockThreshold = overrideBlockThreshold;
        this.flagTtl = Duration.ofMinutes(flagMinutes);
        this.throttleTtl = Duration.ofMinutes(throttleMinutes);
        this.blockTtl = Duration.ofMinutes(blockMinutes);

        Counter.builder("detector_decisions_total")
            .tag("action", "ALLOW")
            .register(meterRegistry);
        Counter.builder("detector_decisions_total")
            .tag("action", "FLAG")
            .register(meterRegistry);
        Counter.builder("detector_decisions_total")
            .tag("action", "THROTTLE")
            .register(meterRegistry);
        Counter.builder("detector_decisions_total")
            .tag("action", "BLOCK")
            .register(meterRegistry);
    }

    public Decision decide(double score, String apiKey, String ip, String triggeredRules) {
        ActionType action = determineAction(score);
        Duration ttl = getTtlForAction(action);
        String reason = String.format("Score: %.2f, Rules: %s", score, triggeredRules);

        Decision decision = new Decision(action, score, reason, Instant.now().plus(ttl));

        saveDecision(apiKey, ip, decision);

        return decision;
    }

    private ActionType determineAction(double score) {
        if (score >= overrideBlockThreshold) {
            return ActionType.BLOCK;
        }
        if (score >= blockThreshold) {
            return ActionType.BLOCK;
        }
        if (score >= throttleThreshold) {
            return ActionType.THROTTLE;
        }
        if (score >= flagThreshold) {
            return ActionType.FLAG;
        }
        return ActionType.ALLOW;
    }

    private Duration getTtlForAction(ActionType action) {
        return switch (action) {
            case ALLOW -> Duration.ofMinutes(1);
            case FLAG -> flagTtl;
            case THROTTLE -> throttleTtl;
            case BLOCK -> blockTtl;
        };
    }

    private void saveDecision(String apiKey, String ip, Decision decision) {
        try {
            if (apiKey != null && !apiKey.isEmpty()) {
                String key = "decision:" + apiKey;
                redisTemplate.opsForHash().putAll(key, Map.of(
                    "action", decision.action().name(),
                    "score", String.valueOf(decision.score()),
                    "reason", decision.reason() != null ? decision.reason() : "",
                    "expiresAt", decision.expiresAt().toString()
                ));
                redisTemplate.expire(key, getTtlForAction(decision.action()));
            }

            if (ip != null && !ip.isEmpty()) {
                String ipKey = "decision:ip:" + ip;
                redisTemplate.opsForHash().putAll(ipKey, Map.of(
                    "action", decision.action().name(),
                    "score", String.valueOf(decision.score()),
                    "reason", decision.reason() != null ? decision.reason() : "",
                    "expiresAt", decision.expiresAt().toString()
                ));
                redisTemplate.expire(ipKey, getTtlForAction(decision.action()));
            }
        } catch (Exception e) {
        }
    }

    public double getWeight(String ruleName) {
        return weights.getOrDefault(ruleName, 0.0);
    }
}