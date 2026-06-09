package com.abuseguard.detector.rules;

import com.abuseguard.common.events.RequestEvent;
import com.abuseguard.detector.engine.Rule;
import com.abuseguard.detector.engine.RuleResult;
import com.abuseguard.detector.store.DetectorRedisStore;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Component
public class UserAgentRule implements Rule {

    private static final Set<String> BLOCKLIST = Set.of(
        "curl", "wget", "python", "scrapy", "bot", "spider"
    );

    private final DetectorRedisStore store;

    public UserAgentRule(DetectorRedisStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "user-agent";
    }

    @Override
    public CompletableFuture<RuleResult> evaluate(RequestEvent event, DetectorRedisStore store) {
        return CompletableFuture.supplyAsync(() -> {
            String userAgent = event.userAgent();
            if (userAgent == null || userAgent.isEmpty()) {
                return RuleResult.triggered(name(), 0.3, "empty user agent");
            }

            String uaLower = userAgent.toLowerCase();
            if (BLOCKLIST.stream().anyMatch(uaLower::contains)) {
                return RuleResult.triggered(name(), 0.8, "blocklisted UA: " + userAgent);
            }

            double entropy = calculateEntropy(userAgent);
            if (entropy < 2.5) {
                return RuleResult.triggered(name(), 0.5, "low entropy: " + entropy);
            }

            return RuleResult.notTriggered();
        });
    }

    static double calculateEntropy(String s) {
        if (s == null || s.isEmpty()) {
            return 0.0;
        }
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : s.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }
        double entropy = 0;
        int len = s.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}