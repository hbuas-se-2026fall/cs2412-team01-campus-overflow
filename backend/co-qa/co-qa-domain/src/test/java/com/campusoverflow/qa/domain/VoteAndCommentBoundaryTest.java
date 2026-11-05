package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** T1-06 领域边界测试：投票取值、自投票、评论长度（按码点）与一层回复的错误码契约。 */
class VoteAndCommentBoundaryTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");

    @Nested
    class Voting {

        @ParameterizedTest
        @ValueSource(ints = {0, 2, -2, Integer.MAX_VALUE, Integer.MIN_VALUE})
        @DisplayName("新建投票只接受 ±1，其余值一律 QA-1006（撤销由应用层删除投票实现）")
        void castRejectsValuesOtherThanPlusMinusOne(int value) {
            assertThatThrownBy(() -> Vote.cast(1, 3, TargetType.ANSWER, 50, value, NOW))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code").isEqualTo("QA-1006");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, -1})
        @DisplayName("给自己的问题或回答投票：QA-1015，消息包含「自己」")
        void selfVoteIsRejectedForBothValues(int value) {
            for (TargetType type : TargetType.values()) {
                assertThatThrownBy(() -> Vote.cast(3, 3, type, 50, value, NOW))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("自己")
                        .extracting("code").isEqualTo("QA-1015");
            }
        }

        @Test
        @DisplayName("自投票优先于取值校验：自己给自己投非法值时报 QA-1015")
        void selfVoteCheckedBeforeValue() {
            assertThatThrownBy(() -> Vote.cast(3, 3, TargetType.QUESTION, 10, 5, NOW))
                    .extracting("code").isEqualTo("QA-1015");
        }

        @Test
        @DisplayName("新建投票：id 为空，记录时间与取值")
        void castInitialState() {
            Vote v = Vote.cast(1, 3, TargetType.ANSWER, 50, -1, NOW);

            assertThat(v.id()).isNull();
            assertThat(v.value()).isEqualTo(-1);
            assertThat(v.updatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("改投为同一值：返回旧值，取值不变")
        void changeToSameValueReturnsPrevious() {
            Vote v = Vote.cast(1, 3, TargetType.ANSWER, 50, 1, NOW);

            assertThat(v.change(1, NOW.plusSeconds(1))).isEqualTo(1);
            assertThat(v.value()).isEqualTo(1);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 2, -2})
        @DisplayName("改投为非法值：QA-1006 且原投票不变")
        void changeRejectsIllegalValues(int value) {
            Vote v = Vote.cast(1, 3, TargetType.ANSWER, 50, -1, NOW);

            assertThatThrownBy(() -> v.change(value, NOW.plusSeconds(1)))
                    .extracting("code").isEqualTo("QA-1006");
            assertThat(v.value()).isEqualTo(-1);
            assertThat(v.updatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    class Commenting {

        private Comment topOnAnswer50() {
            Comment top = Comment.post(TargetType.ANSWER, 50, 10, 1, null, "请问第二步为什么要变色？", NOW);
            top.assignId(1);
            return top;
        }

        @Test
        @DisplayName("正文 1 个字符（下限）可以发布")
        void minimumLengthAccepted() {
            assertThat(Comment.post(TargetType.QUESTION, 10, 10, 1, null, "赞", NOW).body()).isEqualTo("赞");
        }

        @Test
        @DisplayName("正文为 null、空串、纯空白：QA-1002")
        void blankBodyRejected() {
            for (String body : new String[] {null, "", " \t\n "}) {
                assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 1, null, body, NOW))
                        .isInstanceOf(BusinessRuleException.class)
                        .extracting("code").isEqualTo("QA-1002");
            }
        }

        @Test
        @DisplayName("正文 600 / 601 字符边界：600 通过、601 拒绝（QA-1002）")
        void maximumLengthBoundary() {
            assertThat(Comment.post(TargetType.QUESTION, 10, 10, 1, null, "a".repeat(600), NOW).body()).hasSize(600);
            assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 1, null, "a".repeat(601), NOW))
                    .extracting("code").isEqualTo("QA-1002");
        }

        @Test
        @DisplayName("长度按 Unicode 码点计：600 个 emoji（1200 个 char）仍合法")
        void lengthCountedInCodePoints() {
            String emojis = "😀".repeat(600);

            assertThat(Comment.post(TargetType.QUESTION, 10, 10, 1, null, emojis, NOW).body()).isEqualTo(emojis);
            assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 10, 10, 1, null, emojis + "😀", NOW))
                    .extracting("code").isEqualTo("QA-1002");
        }

        @Test
        @DisplayName("首尾空白被去除，且不计入长度")
        void bodyIsStripped() {
            String body = "  " + "a".repeat(600) + "  ";

            assertThat(Comment.post(TargetType.QUESTION, 10, 10, 1, null, body, NOW).body()).isEqualTo("a".repeat(600));
        }

        @Test
        @DisplayName("回复二级评论：QA-1007")
        void replyToReplyRejected() {
            Comment reply = Comment.post(TargetType.ANSWER, 50, 10, 3, topOnAnswer50(), "因为要保持黑高一致。", NOW);
            reply.assignId(2);

            assertThatThrownBy(() -> Comment.post(TargetType.ANSWER, 50, 10, 1, reply, "再回复一层", NOW))
                    .extracting("code").isEqualTo("QA-1007");
        }

        @Test
        @DisplayName("类型相同但 targetId 不同、或 targetId 相同但类型不同：均为 QA-1012")
        void replyOnDifferentTargetRejected() {
            Comment top = topOnAnswer50();

            assertThatThrownBy(() -> Comment.post(TargetType.ANSWER, 51, 10, 3, top, "挂到别的回答下", NOW))
                    .extracting("code").isEqualTo("QA-1012");
            assertThatThrownBy(() -> Comment.post(TargetType.QUESTION, 50, 50, 3, top, "同 id 但类型不同", NOW))
                    .extracting("code").isEqualTo("QA-1012");
        }

        @Test
        @DisplayName("回复尚未持久化（无 id）的父评论属于编程错误")
        void replyToUnsavedParentFails() {
            Comment unsaved = Comment.post(TargetType.ANSWER, 50, 10, 1, null, "还没保存", NOW);

            assertThatThrownBy(() -> Comment.post(TargetType.ANSWER, 50, 10, 3, unsaved, "回复", NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("assignId 只能调用一次")
        void idCanOnlyBeAssignedOnce() {
            Comment top = topOnAnswer50();

            assertThatThrownBy(() -> top.assignId(2)).isInstanceOf(IllegalStateException.class);
        }
    }
}
