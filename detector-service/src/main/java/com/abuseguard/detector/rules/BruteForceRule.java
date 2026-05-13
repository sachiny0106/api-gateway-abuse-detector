package com.abuseguard.detector.rules;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.engine.Rule;
import com.abuseguard.detector.engine.RuleResult;
import com.abuseguard.detector.store.DetectorRedisStore;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class BruteForceRule implements Rule {

    private static final int BRUTE_FORCE_THRESHOLD = 10;

    private final DetectorRedisStore store;

    public BruteForceRule(DetectorRedisStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "brute-force";
    }

    @Override
    public CompletableFuture<RuleResult> evaluate(RequestEvent event, DetectorRedisStore store) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isLoginEndpoint(event.endpoint())) {
                return RuleResult.notTriggered();
            }

            long timestamp = event.timestamp().toEpochMilli();
            String ip = event.ip();
            String endpoint = event.endpoint();

            store.recordBruteForceAttempt(endpoint, ip, timestamp);

            long count = store.getBruteForceCount(endpoint, ip, timestamp);

            boolean triggered = count > BRUTE_FORCE_THRESHOLD;

            if (!triggered) {
                return RuleResult.notTriggered();
            }

            double score = Math.min(1.0, (count - BRUTE_FORCE_THRESHOLD) / 10.0 * 0.5 + 0.5);
            return RuleResult.triggered(name(), score, String.format("%d attempts in 60s", count));
        });
    }

    private boolean isLoginEndpoint(String endpoint) {
        return endpoint != null && endpoint.toLowerCase().contains("login");
    }
}