package com.campusoverflow.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GuardTest {

    @Test
    void requireTextReturnsTrimmedValue() {
        assertThat(Guard.requireText("  跳表  ", 1, 10, "QA-1001", "标题")).isEqualTo("跳表");
    }

    @Test
    void requireTextRejectsNullAndBlank() {
        assertThatThrownBy(() -> Guard.requireText(null, 1, 10, "QA-1001", "标题"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("标题不能为空")
                .extracting("code").isEqualTo("QA-1001");

        assertThatThrownBy(() -> Guard.requireText("   ", 1, 10, "QA-1001", "标题"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void requireTextChecksLengthBoundaries() {
        assertThat(Guard.requireText("a", 1, 3, "QA-1001", "标题")).isEqualTo("a");
        assertThat(Guard.requireText("abc", 1, 3, "QA-1001", "标题")).isEqualTo("abc");

        assertThatThrownBy(() -> Guard.requireText("abcd", 1, 3, "QA-1001", "标题"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("当前 4");
    }

    @Test
    void requireTextCountsByCodePointNotCharUnit() {
        // 一个 emoji 占 2 个 char，但只算 1 个码点：三个 emoji 应当允许出现在 [1, 3] 区间内。
        String threeEmoji = "\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00";
        assertThat(Guard.requireText(threeEmoji, 1, 3, "QA-1001", "昵称")).isEqualTo(threeEmoji);

        assertThatThrownBy(() -> Guard.requireText(threeEmoji + "\uD83D\uDE00", 1, 3, "QA-1001", "昵称"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("当前 4");
    }

    @Test
    void requireNonNullReturnsValueOrThrows() {
        assertThat(Guard.requireNonNull(42L, "QA-1002", "问题")).isEqualTo(42L);

        assertThatThrownBy(() -> Guard.requireNonNull(null, "QA-1002", "问题"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("问题不能为空");
    }
}
