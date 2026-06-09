package com.abuseguard.detector.engine;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * Builds a {@link DecisionMaker} wired with the project's default weights, thresholds and
 * TTLs for use in unit tests. Redis is mocked, so {@code decide(...)} exercises the pure
 * decision logic without touching a real Redis instance.
 */
final class TestDecisionMakerFactory {

    private TestDecisionMakerFactory() {
    }

    static DecisionMaker withDefaults() {
        return new DecisionMaker(
            mock(StringRedisTemplate.class),
            new SimpleMeterRegistry(),
            0.25, 0.35, 0.20, 0.10, 0.15, 0.30,   // weights
            0.45, 0.65, 0.80, 0.90,                // thresholds: flag, throttle, block, override
            5, 15, 60                              // ttl minutes: flag, throttle, block
        );
    }
}
