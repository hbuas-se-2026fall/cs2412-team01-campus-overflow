package com.campusoverflow.reputation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * BadgePolicy 阈值与 AcademicTerm 周期键的边界用例（T2-04）。
 * 时间一律用 UTC 表示，用来验证「先转换到 CAMPUS_ZONE（UTC+8）再取键」。
 */
class BadgePolicyAndAcademicTermBoundaryTest {

    // ---------- BadgePolicy ----------

    @ParameterizedTest(name = "采纳数 {0} → FIRST_ACCEPTED={1}, HELPER_10={2}")
    @CsvSource({"0,false,false", "1,true,false", "9,true,false", "10,true,true", "11,true,true"})
    @DisplayName("采纳数阈值恰在 1 与 10")
    void acceptedCountThresholds(long count, boolean first, boolean helper) {
        assertThat(BadgePolicy.forAcceptedCount(count).contains(BadgeType.FIRST_ACCEPTED)).isEqualTo(first);
        assertThat(BadgePolicy.forAcceptedCount(count).contains(BadgeType.HELPER_10)).isEqualTo(helper);
    }

    @ParameterizedTest(name = "声誉 {0} → REPUTATION_100={1}, REPUTATION_1000={2}")
    @CsvSource({"-12,false,false", "0,false,false", "99,false,false", "100,true,false",
            "999,true,false", "1000,true,true"})
    @DisplayName("声誉阈值恰在 100 与 1000，负声誉不授徽章")
    void reputationThresholds(int reputation, boolean hundred, boolean thousand) {
        assertThat(BadgePolicy.forReputation(reputation).contains(BadgeType.REPUTATION_100)).isEqualTo(hundred);
        assertThat(BadgePolicy.forReputation(reputation).contains(BadgeType.REPUTATION_1000)).isEqualTo(thousand);
    }

    @Test
    @DisplayName("两类规则互不串门：采纳规则只产出采纳徽章，声誉规则只产出声誉徽章")
    void policiesDoNotOverlap() {
        assertThat(BadgePolicy.forAcceptedCount(1000))
                .containsExactlyInAnyOrder(BadgeType.FIRST_ACCEPTED, BadgeType.HELPER_10);
        assertThat(BadgePolicy.forReputation(1000))
                .containsExactlyInAnyOrder(BadgeType.REPUTATION_100, BadgeType.REPUTATION_1000);
    }

    // ---------- AcademicTerm.termKey ----------

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "2026-01-31T15:59:59Z, 2025-FALL",   // 北京时间 1-31 23:59:59 仍属上一年秋季
            "2026-01-31T16:00:00Z, 2026-SPRING", // 北京时间 2-1 00:00 进入春季
            "2026-07-31T15:59:59Z, 2026-SPRING", // 北京时间 7-31 23:59:59
            "2026-07-31T16:00:00Z, 2026-FALL",   // 北京时间 8-1 00:00
            "2026-12-31T15:59:59Z, 2026-FALL",
            "2026-12-31T16:00:00Z, 2026-FALL"    // 北京时间已是 2027-01-01，仍属 2026 秋季
    })
    @DisplayName("学期键在月界与跨年处按北京时间切换")
    void termKeyBoundaries(String utc, String expected) {
        assertThat(AcademicTerm.termKey(Instant.parse(utc))).isEqualTo(expected);
    }

    // ---------- AcademicTerm.weekKey ----------

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "2026-01-04T12:00:00Z, 2026-W01", // 两位补零
            "2026-10-07T12:00:00Z, 2026-W41", // 指导书示例
            "2027-01-01T04:00:00Z, 2026-W53", // 自然年 2027，ISO 周年仍是 2026
            "2024-12-30T04:00:00Z, 2025-W01", // 自然年 2024，ISO 周年已是 2025
            "2026-10-11T15:59:59Z, 2026-W41", // 北京时间周日 23:59:59
            "2026-10-11T16:00:00Z, 2026-W42"  // 北京时间周一 00:00
    })
    @DisplayName("周键按 ISO 周年命名、两位补零，并以北京时间周一零点切换")
    void weekKeyBoundaries(String utc, String expected) {
        assertThat(AcademicTerm.weekKey(Instant.parse(utc))).isEqualTo(expected);
    }

    // ---------- AcademicTerm.monthKey ----------

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "2026-09-30T15:59:59Z, 2026-09",
            "2026-09-30T16:00:00Z, 2026-10", // 北京时间 10-1 00:00
            "2026-12-31T16:00:00Z, 2027-01"  // 月键跟自然年走，与学期键不同
    })
    @DisplayName("月键两位补零，按北京时间切换")
    void monthKeyBoundaries(String utc, String expected) {
        assertThat(AcademicTerm.monthKey(Instant.parse(utc))).isEqualTo(expected);
    }
}
