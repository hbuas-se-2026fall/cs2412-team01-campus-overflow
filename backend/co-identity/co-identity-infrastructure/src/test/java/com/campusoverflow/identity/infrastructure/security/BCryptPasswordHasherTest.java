package com.campusoverflow.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(new BCryptPasswordEncoder(4));

    @Test
    void hashIsNotThePlainPassword() {
        String hash = hasher.hash("Str0ng-Pass!");

        assertThat(hash).isNotEqualTo("Str0ng-Pass!");
        assertThat(hash).startsWith("$2");
    }

    @Test
    void matchesAcceptsCorrectPassword() {
        String hash = hasher.hash("Str0ng-Pass!");

        assertThat(hasher.matches("Str0ng-Pass!", hash)).isTrue();
    }

    @Test
    void matchesRejectsWrongPassword() {
        String hash = hasher.hash("Str0ng-Pass!");

        assertThat(hasher.matches("str0ng-pass!", hash)).isFalse();
        assertThat(hasher.matches("", hash)).isFalse();
    }

    @Test
    void samePasswordHashesDifferentlyEachTime() {
        String first = hasher.hash("SamePass123");
        String second = hasher.hash("SamePass123");

        // 每次加密都带随机盐，避免相同口令产生相同指纹。
        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches("SamePass123", first)).isTrue();
        assertThat(hasher.matches("SamePass123", second)).isTrue();
    }
}
