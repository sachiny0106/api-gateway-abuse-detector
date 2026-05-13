package com.abuseguard.detector.rules;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.engine.Rule;
import com.abuseguard.detector.engine.RuleResult;
import com.abuseguard.detector.store.DetectorRedisStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class BotPatternRule implements Rule {

    private static final double CV_THRESHOLD = 0.2;

    private final DetectorRedisStore store;

    public BotPatternRule(DetectorRedisStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "bot-pattern";
    }

    @Override
    public CompletableFuture<RuleResult> evaluate(RequestEvent event, DetectorRedisStore store) {
        return CompletableFuture.supplyAsync(() -> {
            String apiKey = event.apiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                return RuleResult.notTriggered();
            }

            long timestamp = event.timestamp().toEpochMilli();
            store.recordBotTimestamp(apiKey, timestamp);

            List<String> timestamps = store.getBotTimestamps(apiKey);
            if (timestamps.size() < 3) {
                return RuleResult.notTriggered();
            }

            List<Double> deltas = calculateDeltas(timestamps);
            double mean = calculateMean(deltas);
            double stddev = calculateStddev(deltas, mean);
            double cv = stddev / mean;

            boolean triggered = cv < CV_THRESHOLD && timestamps.size() >= 5;

            if (!triggered) {
                return RuleResult.notTriggered();
            }

            double score = Math.max(0.0, 1.0 - (cv / CV_THRESHOLD));
            return RuleResult.triggered(name(), score, String.format("CV=%.3f, count=%d", cv, timestamps.size()));
        });
    }

    private List<Double> calculateDeltas(List<String> timestamps) {
        List<Double> deltas = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (String ts : timestamps) {
            try {
                values.add(Long.parseLong(ts));
            } catch (NumberFormatException e) {
            }
        }
        for (int i = 1; i < values.size(); i++) {
            deltas.add((double) (values.get(i) - values.get(i - 1)));
        }
        return deltas;
    }

    private double calculateMean(List<Double> values) {
        if (values.isEmpty()) return 0;
        return values.stream().mapToDouble(Double::doubleValue).sum() / values.size();
    }

    private double calculateStddev(List<Double> values, double mean) {
        if (values.isEmpty()) return 0;
        double sqSum = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum();
        return Math.sqrt(sqSum / values.size());
    }
}