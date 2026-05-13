package com.abuseguard.detector.engine;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.producer.DecisionEventProducer;
import com.abuseguard.detector.store.DetectorRedisStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class DetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(DetectionEngine.class);

    private final List<Rule> rules;
    private final DetectorRedisStore store;
    private final Scorer scorer;
    private final DecisionMaker decisionMaker;
    private final DecisionEventProducer producer;

    public DetectionEngine(
            List<Rule> rules,
            DetectorRedisStore store,
            Scorer scorer,
            DecisionMaker decisionMaker,
            DecisionEventProducer producer,
            MeterRegistry meterRegistry
    ) {
        this.rules = rules;
        this.store = store;
        this.scorer = scorer;
        this.decisionMaker = decisionMaker;
        this.producer = producer;

        for (String ruleName : List.of("spike", "brute-force", "bot-pattern", "user-agent", "ip-behavior", "sequence")) {
            Counter.builder("detector_rule_triggered_total")
                .tag("rule", ruleName)
                .register(meterRegistry);
        }
    }

    public void process(RequestEvent event) {
        List<CompletableFuture<RuleResult>> futures = new ArrayList<>();

        for (Rule rule : rules) {
            CompletableFuture<RuleResult> future = rule.evaluate(event, store);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<RuleResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

                for (RuleResult result : results) {
                    if (result.triggered()) {
                        String ruleName = result.reason().split(":")[0].trim();
                        Counter.builder("detector_rule_triggered_total")
                            .tag("rule", ruleName)
                            .register(io.micrometer.core.instrument.Metrics.globalRegistry)
                            .increment();
                    }
                }

                return results;
            })
            .thenAccept(results -> {
                double score = scorer.aggregate(results, decisionMaker);
                String triggeredRules = scorer.formatTriggeredRules(results);

                if (score > 0) {
                    var decision = decisionMaker.decide(score, event.apiKey(), event.ip(), triggeredRules);
                    producer.publish(event, decision, triggeredRules);
                    log.info("Decision made: {} for IP {} / API key {} (score: {})",
                        decision.action(), event.ip(), event.apiKey(), score);
                }
            })
            .exceptionally(ex -> {
                log.error("Error processing event {}: {}", event.eventId(), ex.getMessage());
                return null;
            });
    }
}