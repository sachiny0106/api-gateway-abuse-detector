package com.abuseguard.detector.engine;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Scorer {

    public double aggregate(List<RuleResult> results, DecisionMaker decisionMaker) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (RuleResult result : results) {
            if (result.triggered()) {
                double weight = decisionMaker.getWeight(result.reason().split(":")[0].trim());
                weightedSum += result.score() * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0.0) {
            return 0.0;
        }

        return Math.min(1.0, weightedSum / totalWeight);
    }

    public String formatTriggeredRules(List<RuleResult> results) {
        return results.stream()
            .filter(RuleResult::triggered)
            .map(r -> r.reason().split(":")[0].trim())
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse("none");
    }
}