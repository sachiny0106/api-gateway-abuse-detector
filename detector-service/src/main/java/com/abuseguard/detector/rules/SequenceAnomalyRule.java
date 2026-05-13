package com.abuseguard.detector.rules;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.engine.Rule;
import com.abuseguard.detector.engine.RuleResult;
import com.abuseguard.detector.store.DetectorRedisStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class SequenceAnomalyRule implements Rule {

    private final DetectorRedisStore store;
    private final Map<String, SequenceConfig> sequences;

    public SequenceAnomalyRule(
            DetectorRedisStore store,
            MeterRegistry meterRegistry,
            @Value("${abuse-detector.sequences.account_takeover.pattern:}") String accountTakeoverPattern,
            @Value("${abuse-detector.sequences.data_harvest.pattern:}") String dataHarvestPattern
    ) {
        this.store = store;

        List<SequenceConfig> configs = new ArrayList<>();

        if (!accountTakeoverPattern.isEmpty()) {
            configs.add(new SequenceConfig("account_takeover",
                List.of(accountTakeoverPattern.split(",")),
                0.9, 10));
        }

        if (!dataHarvestPattern.isEmpty()) {
            configs.add(new SequenceConfig("data_harvest",
                List.of(dataHarvestPattern.split(",")),
                0.8, 5));
        }

        this.sequences = configs.stream()
            .collect(Collectors.toMap(SequenceConfig::name, c -> c));
    }

    @Override
    public String name() {
        return "sequence";
    }

    @Override
    public CompletableFuture<RuleResult> evaluate(RequestEvent event, DetectorRedisStore store) {
        return CompletableFuture.supplyAsync(() -> {
            String apiKey = event.apiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                return RuleResult.notTriggered();
            }

            store.recordSequence(apiKey, event.endpoint());

            List<String> sequence = store.getSequence(apiKey);
            if (sequence.size() < 2) {
                return RuleResult.notTriggered();
            }

            for (SequenceConfig config : sequences.values()) {
                if (matches(config, sequence)) {
                    return RuleResult.triggered(name(), config.score, config.name() + " sequence detected");
                }
            }

            return RuleResult.notTriggered();
        });
    }

    private boolean matches(SequenceConfig config, List<String> actual) {
        List<String> pattern = config.pattern();
        if (pattern.size() > actual.size()) {
            return false;
        }

        for (int i = 0; i <= actual.size() - pattern.size(); i++) {
            boolean match = true;
            for (int j = 0; j < pattern.size(); j++) {
                if (!actual.get(i + j).equals(pattern.get(j))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    record SequenceConfig(String name, List<String> pattern, double score, int windowMinutes) {}
}