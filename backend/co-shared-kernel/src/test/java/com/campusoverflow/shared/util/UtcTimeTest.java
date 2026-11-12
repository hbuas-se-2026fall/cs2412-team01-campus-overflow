package com.campusoverflow.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UtcTimeTest {

    private static final Instant MOMENT = Instant.parse("2026-11-12T04:30:00Z");

    private final TimeZone original = TimeZone.getDefault();

    @AfterEach
    void restoreDefaultTimeZone() {
        TimeZone.setDefault(original);
    }

    @Test
    void toDbStoresUtcWallClock() {
        assertThat(UtcTime.toDb(MOMENT)).isEqualTo(LocalDateTime.of(2026, 11, 12, 4, 30));
    }

    @Test
    void fromDbTreatsValueAsUtc() {
        assertThat(UtcTime.fromDb(LocalDateTime.of(2026, 11, 12, 4, 30))).isEqualTo(MOMENT);
    }

    @Test
    void roundTripKeepsInstant() {
        assertThat(UtcTime.fromDb(UtcTime.toDb(MOMENT))).isEqualTo(MOMENT);
    }

    @Test
    void conversionDoesNotDependOnJvmDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        LocalDateTime inShanghai = UtcTime.toDb(MOMENT);

        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        LocalDateTime inNewYork = UtcTime.toDb(MOMENT);

        assertThat(inShanghai).isEqualTo(inNewYork).isEqualTo(LocalDateTime.of(2026, 11, 12, 4, 30));
    }

    @Test
    void nullsPassThrough() {
        assertThat(UtcTime.toDb(null)).isNull();
        assertThat(UtcTime.fromDb(null)).isNull();
    }
}
