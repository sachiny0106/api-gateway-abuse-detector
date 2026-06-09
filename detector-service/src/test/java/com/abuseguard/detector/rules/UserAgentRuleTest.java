package com.abuseguard.detector.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentRuleTest {

    @Test
    @DisplayName("entropy of empty or null input is zero")
    void emptyEntropyIsZero() {
        assertThat(UserAgentRule.calculateEntropy("")).isZero();
        assertThat(UserAgentRule.calculateEntropy(null)).isZero();
    }

    @Test
    @DisplayName("a single repeated character has zero entropy")
    void uniformStringHasZeroEntropy() {
        assertThat(UserAgentRule.calculateEntropy("aaaaaaaa")).isZero();
    }

    @Test
    @DisplayName("two equally-likely symbols give exactly 1 bit of entropy")
    void twoSymbolsGiveOneBit() {
        assertThat(UserAgentRule.calculateEntropy("abab"))
            .isEqualTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("non-ASCII characters are counted distinctly (no 8-bit collisions)")
    void nonAsciiCharactersAreDistinct() {
        // U+0061 'a' and U+0161 'š' differ only in their high byte. The old
        // (c & 0xFF) implementation collapsed them to the same bucket, wrongly
        // reporting zero entropy. A correct implementation sees two symbols.
        double entropy = UserAgentRule.calculateEntropy("ašaš");
        assertThat(entropy).isEqualTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("a realistic browser UA has higher entropy than a trivial one")
    void realisticUaHasHigherEntropy() {
        double trivial = UserAgentRule.calculateEntropy("aaaa");
        double browser = UserAgentRule.calculateEntropy(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        assertThat(browser).isGreaterThan(trivial);
    }
}
