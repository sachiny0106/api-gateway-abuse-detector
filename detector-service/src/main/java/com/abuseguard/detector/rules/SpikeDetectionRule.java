package com.abuseguard.detector.rules;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.engine.Rule;
import com.abuseguard.detector.engine.RuleResult;
import com.abuseguard.detector.store.DetectorRedisStore;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class SpikeDetectionRule implements Rule {

    private final DetectorRedisStore store;

    public SpikeDetectionRule(DetectorRedisStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "spike";
    }

    @Override
    public CompletableFuture<RuleResult> evaluate(RequestEvent event, DetectorRedisStore store) {
        return CompletableFuture.supplyAsync(() -> {
            long timestamp = event.timestamp().toEpochMilli();
            String ip = event.ip();

            store.recordSpikeData(ip, timestamp);

            long count10s = store.getSpikeShortCount(ip, timestamp);
            long count60s = store.getSpikeLongCount(ip, timestamp);

            if (count10s == 0 || count60s == 0) {
                return RuleResult.notTriggered();
            }

            double shortRate = count10s / 10.0;
            double longRate = count60s / 60.0;

            boolean triggered = shortRate > 3 * longRate && count10s > 20;

            if (!triggered) {
                return RuleResult.notTriggered();
            }

            double score = Math.min(1.0, (shortRate / longRate) / 5.0);
            return RuleResult.triggered(name(), score, String.format("shortRate=%.2f, longRate=%.2f, count10s=%d", shortRate, longRate, count10s));
        });
    }
}