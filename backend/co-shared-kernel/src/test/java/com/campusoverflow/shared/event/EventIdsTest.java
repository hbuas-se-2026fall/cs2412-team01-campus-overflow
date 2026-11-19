package com.campusoverflow.shared.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventIdsTest {

    @Test
    void nextReturnsUuidFormat() {
        String id = EventIds.next();
        assertThat(id).hasSize(36);
        assertThat(UUID.fromString(id).toString()).isEqualTo(id);
    }

    @Test
    void nextIsUniqueAcrossManyCalls() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 5_000; i++) {
            assertThat(seen.add(EventIds.next())).isTrue();
        }
        assertThat(seen).hasSize(5_000);
    }
}
