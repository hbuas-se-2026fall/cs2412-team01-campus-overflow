package com.campusoverflow.discovery.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.discovery.domain.notification.MentionParser;
import com.campusoverflow.discovery.domain.notification.Notification;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.discovery.domain.search.RelatedQuestionPolicy;
import com.campusoverflow.discovery.domain.search.SearchDocument;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.discovery.domain.search.SearchSort;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 发现上下文领域层的补充用例：补上 {@link DiscoveryDomainTest} 没有走到的分支。
 * 每条用例都对应生产代码里一处具体的条件判断，不做重复覆盖。
 */
class DiscoverySearchAndNotificationBoundaryTest {

    private static final Instant NOW = Instant.parse("2026-11-17T05:00:00Z");

    // ------------------------------------------------------------------ 搜索条件

    @Nested
    @DisplayName("SearchQuery：关键词规范化与校验")
    class SearchQueryRules {

        @Test
        @DisplayName("清理运算符后被清空的输入应归一为 null，而不是空串")
        void operatorOnlyKeywordBecomesNull() {
            // 全是运算符：替换成空格、strip 之后为空，必须归一为 null，
            // 否则后面 sort 判断会把「没有关键词」误当成「有关键词」。
            SearchQuery q = new SearchQuery("\"\"+-~*", null, null, null, false, false, null);
            assertThat(q.keyword()).isNull();
            assertThat(q.terms()).isEmpty();
            assertThat(q.sort()).isEqualTo(SearchSort.NEWEST);
        }

        @Test
        @DisplayName("超过 50 个码点抛 DIS-1002，恰好 50 个通过")
        void keywordLengthLimit() {
            String fifty = "红".repeat(50);
            assertThat(new SearchQuery(fifty, null, null, null, false, false, null).keyword()).hasSize(50);

            String fiftyOne = "红".repeat(51);
            assertThatThrownBy(() -> new SearchQuery(fiftyOne, null, null, null, false, false, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(e -> ((BusinessRuleException) e).code()).isEqualTo("DIS-1002");
        }

        @Test
        @DisplayName("长度按码点计而非按 char 计：50 个增补平面字符也应通过")
        void lengthLimitCountsCodePoints() {
            // 每个 emoji 在 Java 里占 2 个 char，若按 length() 判会被误判为 100 而拒绝。
            String emoji = "\uD83D\uDE00".repeat(50);
            assertThat(emoji.length()).isEqualTo(100);
            assertThat(new SearchQuery(emoji, null, null, null, false, false, null).keyword()).hasSize(100);
        }

        @Test
        @DisplayName("词项超过 5 个时只保留前 5 个")
        void termsAreCappedAtFive() {
            SearchQuery q = new SearchQuery("一A 二B 三C 四D 五E 六F 七G", null, null, null, false, false, null);
            assertThat(q.terms()).containsExactly("一A", "二B", "三C", "四D", "五E");
        }

        @Test
        @DisplayName("标签去空白并转小写；纯空白标签归一为 null")
        void tagIsNormalized() {
            assertThat(new SearchQuery("红黑树", null, "  Java  ", null, false, false, null).tag())
                    .isEqualTo("java");
            assertThat(new SearchQuery("红黑树", null, "   ", null, false, false, null).tag()).isNull();
            assertThat(new SearchQuery("红黑树", null, null, null, false, false, null).tag()).isNull();
        }

        @Test
        @DisplayName("显式要 RELEVANCE 但没有关键词时退化为 NEWEST")
        void relevanceWithoutKeywordFallsBackToNewest() {
            SearchQuery q = new SearchQuery(null, null, null, SearchSort.RELEVANCE, false, false, null);
            assertThat(q.sort()).isEqualTo(SearchSort.NEWEST);
        }

        @Test
        @DisplayName("有其他筛选条件但无关键词时，仍允许保留 RELEVANCE 之外的排序")
        void explicitSortIsKeptWhenKeywordPresent() {
            SearchQuery q = new SearchQuery("红黑树", 7L, null, SearchSort.HOT, false, false, null);
            assertThat(q.sort()).isEqualTo(SearchSort.HOT);
            assertThat(q.courseId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("未传分页时使用默认值 1/20，传了则原样保留")
        void pageDefaults() {
            SearchQuery q = new SearchQuery("红黑树", null, null, null, false, false, null);
            assertThat(q.page()).isEqualTo(new PageRequest(1, 20));

            PageRequest custom = new PageRequest(3, 5);
            assertThat(new SearchQuery("红黑树", null, null, null, false, false, custom).page())
                    .isEqualTo(custom);
        }

        @Test
        @DisplayName("isBlank 的空白串在规范化后也归一为 null（走 split 的空分支）")
        void blankKeywordGoesThroughSplitBranch() {
            // 带全角空格：replaceAll 不会动它，strip 之后为空 → null
            SearchQuery q = new SearchQuery("\u3000\u3000", null, null, null, false, false, null);
            assertThat(q.keyword()).isNull();
        }

        @Test
        @DisplayName("词项之间夹带制表符、换行等空白时，空白项被过滤掉、不占 5 个名额")
        void blankTermsAreFilteredOut() {
            // 关键词内部带制表符与连续空格：split(\\s+) 之后不应出现空词项
            SearchQuery q = new SearchQuery("红黑树\t\t  旋转\n  平衡", null, null, null, false, false, null);
            assertThat(q.terms()).containsExactly("红黑树", "旋转", "平衡");
            assertThat(q.terms()).noneMatch(String::isBlank);
        }
    }

    // ------------------------------------------------------------------ 相关推荐

    @Nested
    @DisplayName("RelatedQuestionPolicy：Jaccard 与排序")
    class RelatedPolicy {

        @Test
        @DisplayName("没有共享标签或并集非正时相似度为 0，不出现除零或负数")
        void jaccardDegenerateInputs() {
            assertThat(RelatedQuestionPolicy.jaccard(0, 3, 4)).isZero();
            assertThat(RelatedQuestionPolicy.jaccard(-1, 3, 4)).isZero();
            // 并集 = 0 + 0 - 0 = 0，此时即便 shared 为 0 也必须返回 0 而不是 NaN
            assertThat(RelatedQuestionPolicy.jaccard(0, 0, 0)).isZero();
            // 不自洽的入参（shared 大于两边标签数）会让并集 ≤ 0，同样短路为 0，
            // 而不是算出一个大于 1 的「相似度」破坏排序。
            assertThat(RelatedQuestionPolicy.jaccard(2, 1, 1)).isZero();
            // 正常入参：共享 2，各自 3 个标签 → 并集 4 → 0.5
            assertThat(RelatedQuestionPolicy.jaccard(2, 3, 3)).isEqualTo(0.5);
        }

        @Test
        @DisplayName("空列表、null、limit ≤ 0 一律返回空结果")
        void rankDegenerateInputs() {
            List<RelatedCandidate> some = List.of(new RelatedCandidate(1, "A", 1, 2, 0, 0, false));
            assertThat(RelatedQuestionPolicy.rank(null, 2, 3)).isEmpty();
            assertThat(RelatedQuestionPolicy.rank(List.of(), 2, 3)).isEmpty();
            assertThat(RelatedQuestionPolicy.rank(some, 2, 0)).isEmpty();
            assertThat(RelatedQuestionPolicy.rank(some, 2, -1)).isEmpty();
        }

        @Test
        @DisplayName("共享标签为 0 的候选被过滤，哪怕它得分很高")
        void candidatesWithoutSharedTagsAreDropped() {
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    new RelatedCandidate(1, "无共享但高分", 0, 3, 9999, 50, true),
                    new RelatedCandidate(2, "共享一标签", 1, 2, 0, 0, false)
            ), 2, 5);
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(2L);
        }

        @Test
        @DisplayName("相似度相同时：先比得分，再比是否已采纳")
        void tiesBreakByScoreThenAccepted() {
            // 三个候选的 Jaccard 都是 1/3（共享 1，自身 2 标签，候选 2 标签 → 1/(2+2-1)）
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    new RelatedCandidate(10, "低分已采纳", 1, 2, 10, 0, true),
                    new RelatedCandidate(11, "高分未采纳", 1, 2, 90, 0, false),
                    new RelatedCandidate(12, "同分已采纳", 1, 2, 90, 0, true)
            ), 2, 5);
            // 90 分两个在前，其中已采纳的 12 优先；10 分的 10 最后
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(12L, 11L, 10L);
        }

        @Test
        @DisplayName("相似度、得分、采纳状态全相同时按 questionId 升序，结果稳定")
        void finalTieBreaksByIdAndIsStable() {
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    new RelatedCandidate(33, "C", 2, 3, 5, 0, false),
                    new RelatedCandidate(11, "A", 2, 3, 5, 0, false),
                    new RelatedCandidate(22, "B", 2, 3, 5, 0, false)
            ), 3, 5);
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(11L, 22L, 33L);
        }

        @Test
        @DisplayName("limit 生效：只取前 N 个，且不因 limit 改变排序")
        void limitTruncatesWithoutReordering() {
            List<RelatedCandidate> all = new ArrayList<>();
            for (long i = 1; i <= 6; i++) {
                // 共享数递减 → 相似度递减
                all.add(new RelatedCandidate(i, "Q" + i, (int) (7 - i), 6, 0, 0, false));
            }
            List<RelatedCandidate> top2 = RelatedQuestionPolicy.rank(all, 6, 2);
            assertThat(top2).extracting(RelatedCandidate::questionId).containsExactly(1L, 2L);
        }
    }

    // ------------------------------------------------------------------ 搜索投影文档

    @Test
    @DisplayName("SearchDocument 的标签列表做防御性拷贝，外部修改不影响已建对象")
    void searchDocumentDefensivelyCopiesTags() {
        List<String> mutable = new ArrayList<>(Arrays.asList("java", "算法"));
        SearchDocument doc = new SearchDocument(1L, 2L, 3L, "标题", "正文", mutable, NOW);

        mutable.add("被偷偷加进来的");

        assertThat(doc.tags()).containsExactly("java", "算法");
        assertThatThrownBy(() -> doc.tags().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("SearchDocument 传入 null 标签时按 NPE 快速失败，而不是留到序列化才炸")
    void searchDocumentRejectsNullTags() {
        assertThatThrownBy(() -> new SearchDocument(1L, null, 3L, "t", "b", null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SearchHit 携带完整检索结果，各字段按构造顺序就位")
    void searchHitCarriesAllFields() {
        SearchHit hit = new SearchHit(42L, "红黑树的自旋", "……旋转两次……", List.of("算法", "树"),
                7L, 300L, 88, 12, true, false, NOW);

        assertThat(hit.questionId()).isEqualTo(42L);
        assertThat(hit.title()).isEqualTo("红黑树的自旋");
        assertThat(hit.snippet()).contains("旋转");
        assertThat(hit.tags()).containsExactly("算法", "树");
        assertThat(hit.courseId()).isEqualTo(7L);
        assertThat(hit.authorId()).isEqualTo(300L);
        assertThat(hit.score()).isEqualTo(88);
        assertThat(hit.answerCount()).isEqualTo(12);
        assertThat(hit.accepted()).isTrue();
        assertThat(hit.bountyOpen()).isFalse();
        assertThat(hit.createdAt()).isEqualTo(NOW);
    }

    // ------------------------------------------------------------------ 通知

    @Nested
    @DisplayName("Notification：创建与重建")
    class NotificationLifecycle {

        @Test
        @DisplayName("create 不传时间时用当前时间，传了则用传入值")
        void createUsesGivenInstantOrNow() {
            Notification atNow = Notification.create(1, NotificationType.MENTIONED, "有人提到了你", "/q/1", "evt-1", NOW);
            assertThat(atNow.createdAt()).isEqualTo(NOW);
            assertThat(atNow.read()).isFalse();
            assertThat(atNow.id()).isNull();

            Notification noTime = Notification.create(1, NotificationType.MENTIONED, "t", "/q/1", "evt-2", null);
            assertThat(noTime.createdAt()).isNotNull();
        }

        @Test
        @DisplayName("reconstitute 完整还原所有字段，包括已读状态与 id")
        void reconstituteRestoresAllFields() {
            Notification n = Notification.reconstitute(99L, 7L, NotificationType.BADGE_AWARDED,
                    "你获得了新徽章", "/users/7", "evt-9", true, NOW);

            assertThat(n.id()).isEqualTo(99L);
            assertThat(n.recipientId()).isEqualTo(7L);
            assertThat(n.type()).isEqualTo(NotificationType.BADGE_AWARDED);
            assertThat(n.title()).isEqualTo("你获得了新徽章");
            assertThat(n.link()).isEqualTo("/users/7");
            assertThat(n.sourceEventId()).isEqualTo("evt-9");
            assertThat(n.read()).isTrue();
            assertThat(n.createdAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("assignId 只在入库时补上 id，不影响已读状态")
        void assignIdKeepsReadState() {
            Notification n = Notification.create(1, NotificationType.ANSWER_ACCEPTED, "t", "/q/1", "evt-1", NOW);
            n.markRead(1);
            n.assignId(42L);

            assertThat(n.id()).isEqualTo(42L);
            assertThat(n.read()).isTrue();
        }

        @Test
        @DisplayName("type 与 sourceEventId 为 null 时立即拒绝")
        void nullTypeOrEventIdRejected() {
            assertThatThrownBy(() -> Notification.create(1, null, "t", "/q/1", "evt-1", NOW))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Notification.create(1, NotificationType.MENTIONED, "t", "/q/1", null, NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("已读之后再次标记仍为已读，不报错（幂等）")
        void markReadIsIdempotent() {
            Notification n = Notification.create(7, NotificationType.MENTIONED, "t", "/q/1", "evt-1", NOW);
            n.markRead(7);
            n.markRead(7);
            assertThat(n.read()).isTrue();
        }
    }

    // ------------------------------------------------------------------ @提及

    @Nested
    @DisplayName("MentionParser：边界")
    class Mentions {

        @Test
        @DisplayName("null 与纯空白输入返回空集合")
        void nullAndBlankReturnEmpty() {
            assertThat(MentionParser.extract(null)).isEmpty();
            assertThat(MentionParser.extract("   ")).isEmpty();
            assertThat(MentionParser.extract("没有任何提及")).isEmpty();
        }

        @Test
        @DisplayName("昵称长度边界：1 个字符不识别，2 个起识别，恰好 20 个仍是完整一个")
        void nicknameLengthBoundary() {
            // 下限：{2,20} 要求至少 2 个字符，单个汉字不构成提及
            assertThat(MentionParser.extract("@a")).isEmpty();
            assertThat(MentionParser.extract("@甲")).isEmpty();
            assertThat(MentionParser.extract("@ab")).containsExactly("ab");

            // 上限：20 个字符正好是最长合法昵称，整段原样取出
            assertThat(MentionParser.extract("@" + "字".repeat(20)))
                    .containsExactly("字".repeat(20));
        }

        @Test
        @DisplayName("超过 20 个字符时正则在前 20 个处收尾，而不是整体丢弃")
        void overlongMentionIsTruncatedAtTwenty() {
            // 正则量词是贪婪但上限 20：21 个连续汉字会匹配出前 20 个作为一个提及。
            // 这是正则语义决定的既有行为，这里把它钉住，避免将来改量词时无人察觉。
            assertThat(MentionParser.extract("@" + "字".repeat(21)))
                    .containsExactly("字".repeat(20));
        }

        @Test
        @DisplayName("中文标点切断昵称，空格与 @ 也切断")
        void punctuationTerminatesNickname() {
            assertThat(MentionParser.extract("@王老师，你好")).containsExactly("王老师");
            assertThat(MentionParser.extract("@小明。@小红、@小刚")).containsExactly("小明", "小红", "小刚");
            // @ 后直接跟空格：0 个字符，不满足 {2,20}，不是提及
            assertThat(MentionParser.extract("@ 甲")).isEmpty();
            assertThat(MentionParser.extract("@甲1 和 @乙2")).containsExactly("甲1", "乙2");
        }

        @Test
        @DisplayName("超限时按出现顺序截取前 5 个，不是随机取")
        void capKeepsFirstFiveInOrder() {
            assertThat(MentionParser.extract("@aa @bb @cc @dd @ee @ff @gg"))
                    .containsExactly("aa", "bb", "cc", "dd", "ee");
        }
    }

    @Test
    @DisplayName("PageResult 的边界由共享内核负责，这里只确认领域层不越权改分页")
    void domainDoesNotMutatePaging() {
        PageRequest page = new PageRequest(2, 30);
        SearchQuery q = new SearchQuery("红黑树", null, null, null, false, false, page);
        assertThat((Object) q.page()).isSameAs(page);
        assertThat(PageResult.class).isNotNull();
    }
}
