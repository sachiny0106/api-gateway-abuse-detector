package com.abuseguard.detector.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SequenceAnomalyRuleTest {

    @Test
    @DisplayName("splits a clean comma-separated pattern")
    void parsesCleanPattern() {
        assertThat(SequenceAnomalyRule.parsePattern("/api/login,/api/change-email,/api/change-password"))
            .containsExactly("/api/login", "/api/change-email", "/api/change-password");
    }

    @Test
    @DisplayName("trims whitespace around each endpoint so exact matching still works")
    void trimsWhitespaceBetweenEntries() {
        // Without trimming, " /api/change-email" would never match the recorded
        // endpoint "/api/change-email" and the rule would silently never fire.
        assertThat(SequenceAnomalyRule.parsePattern("/api/login, /api/change-email ,  /api/change-password"))
            .containsExactly("/api/login", "/api/change-email", "/api/change-password");
    }

    @Test
    @DisplayName("drops empty tokens from trailing or doubled commas")
    void dropsEmptyTokens() {
        assertThat(SequenceAnomalyRule.parsePattern("/api/login,,/api/export,"))
            .containsExactly("/api/login", "/api/export");
    }

    @Test
    @DisplayName("a single endpoint produces a one-element pattern")
    void singleEndpoint() {
        assertThat(SequenceAnomalyRule.parsePattern("/api/login"))
            .isEqualTo(List.of("/api/login"));
    }
}
