package com.abuseguard.detector.engine;

import com.abuseguard.common.model.ActionType;
import com.abuseguard.common.model.Decision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionMakerTest {

    private final DecisionMaker decisionMaker = TestDecisionMakerFactory.withDefaults();

    @ParameterizedTest(name = "score {0} -> {1}")
    @CsvSource({
        "0.00, ALLOW",
        "0.44, ALLOW",
        "0.45, FLAG",
        "0.64, FLAG",
        "0.65, THROTTLE",
        "0.79, THROTTLE",
        "0.80, BLOCK",
        "0.89, BLOCK",
        "0.90, BLOCK",
        "1.00, BLOCK"
    })
    @DisplayName("maps score to the correct action at every threshold boundary")
    void scoreMapsToAction(double score, ActionType expected) {
        Decision decision = decisionMaker.decide(score, "api-key", "1.2.3.4", "test");
        assertThat(decision.action()).isEqualTo(expected);
        assertThat(decision.score()).isEqualTo(score);
    }

    @Test
    @DisplayName("decision carries a non-null reason and a future expiry")
    void decisionHasReasonAndExpiry() {
        Decision decision = decisionMaker.decide(0.85, "api-key", "1.2.3.4", "brute-force");
        assertThat(decision.reason()).isNotNull().contains("brute-force");
        assertThat(decision.expiresAt()).isNotNull().isAfter(java.time.Instant.now());
    }

    @Test
    @DisplayName("getWeight returns configured weights and 0 for unknown rules")
    void getWeightReturnsConfiguredValues() {
        assertThat(decisionMaker.getWeight("brute-force")).isEqualTo(0.35);
        assertThat(decisionMaker.getWeight("spike")).isEqualTo(0.25);
        assertThat(decisionMaker.getWeight("unknown")).isZero();
    }

    @Test
    @DisplayName("a Redis failure does not prevent a decision from being returned (fail-safe)")
    void redisFailureDoesNotBreakDecision() {
        // The factory wires a mocked StringRedisTemplate whose opsForHash() returns null,
        // so saveDecision() throws internally; decide() must still return the decision.
        Decision decision = decisionMaker.decide(0.95, "api-key", "1.2.3.4", "sequence");
        assertThat(decision.action()).isEqualTo(ActionType.BLOCK);
    }
}
