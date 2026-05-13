package com.abuseguard.detector.rules;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.engine.Rule;
import com.abuseguard.detector.engine.RuleResult;
import com.abuseguard.detector.store.DetectorRedisStore;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class IpBehaviorRule implements Rule {

    private static final int UNIQUE_ENDPOINT_THRESHOLD = 20;

    private final DetectorRedisStore store;

    public IpBehaviorRule(DetectorRedisStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "ip-behavior";
    }

    @Override
    public CompletableFuture<RuleResult> evaluate(RequestEvent event, DetectorRedisStore store) {
        return CompletableFuture.supplyAsync(() -> {
            String ip = event.ip();
            String endpoint = event.endpoint();

            if (ip == null || endpoint == null) {
                return RuleResult.notTriggered();
            }

            long timestamp = event.timestamp().toEpochMilli();
            store.recordIpEndpoint(ip, endpoint, timestamp);

            long uniqueEndpoints = store.getIpUniqueEndpoints(ip, timestamp);
            boolean triggered = uniqueEndpoints > UNIQUE_ENDPOINT_THRESHOLD;

            if (!triggered) {
                return RuleResult.notTriggered();
            }

            double score = Math.min(1.0, (uniqueEndpoints - UNIQUE_ENDPOINT_THRESHOLD) / 30.0 * 0.5 + 0.5);
            return RuleResult.triggered(name(), score, String.format("%d unique endpoints", uniqueEndpoints));
        });
    }
}