package com.abuseguard.detector.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScorerTest {

    private final Scorer scorer = new Scorer();
    private final DecisionMaker decisionMaker = TestDecisionMakerFactory.withDefaults();

    @Test
    @DisplayName("returns 0 when no results are supplied")
    void emptyResultsScoreZero() {
        assertThat(scorer.aggregate(List.of(), decisionMaker)).isZero();
        assertThat(scorer.aggregate(null, decisionMaker)).isZero();
    }

    @Test
    @DisplayName("ignores rules that did not trigger")
    void nonTriggeredRulesAreIgnored() {
        double score = scorer.aggregate(
            List.of(RuleResult.notTriggered(), RuleResult.notTriggered()),
            decisionMaker);
        assertThat(score).isZero();
    }

    @Test
    @DisplayName("a single triggered rule yields its own score regardless of weight")
    void singleRuleEqualsItsScore() {
        // weighted average of one rule = (score*weight)/weight = score
        double score = scorer.aggregate(
            List.of(RuleResult.triggered("brute-force", 0.7, "x")),
            decisionMaker);
        assertThat(score).isEqualTo(0.7, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("combines triggered rules as a weight-normalized average")
    void weightedAverageAcrossRules() {
        // brute-force weight 0.35 @ 1.0, user-agent weight 0.10 @ 0.5
        // (1.0*0.35 + 0.5*0.10) / (0.35 + 0.10) = 0.40 / 0.45 = 0.888...
        double score = scorer.aggregate(
            List.of(
                RuleResult.triggered("brute-force", 1.0, "x"),
                RuleResult.triggered("user-agent", 0.5, "y")
            ),
            decisionMaker);
        assertThat(score).isEqualTo(0.40 / 0.45, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("never exceeds 1.0")
    void scoreIsCappedAtOne() {
        double score = scorer.aggregate(
            List.of(
                RuleResult.triggered("brute-force", 1.0, "x"),
                RuleResult.triggered("sequence", 1.0, "y")
            ),
            decisionMaker);
        assertThat(score).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("unknown rule names contribute zero weight and are effectively ignored")
    void unknownRuleNameHasZeroWeight() {
        double score = scorer.aggregate(
            List.of(RuleResult.triggered("does-not-exist", 0.9, "x")),
            decisionMaker);
        assertThat(score).isZero();
    }

    @Test
    @DisplayName("formatTriggeredRules lists distinct triggered rule names")
    void formatsTriggeredRuleNames() {
        String formatted = scorer.formatTriggeredRules(List.of(
            RuleResult.triggered("brute-force", 1.0, "a"),
            RuleResult.triggered("brute-force", 1.0, "b"),
            RuleResult.notTriggered(),
            RuleResult.triggered("spike", 0.8, "c")
        ));
        assertThat(formatted).contains("brute-force").contains("spike");
        // distinct: brute-force should appear once
        assertThat(formatted.split("brute-force")).hasSize(2);
    }
}
