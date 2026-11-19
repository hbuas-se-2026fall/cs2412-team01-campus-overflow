package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.RecordingSearchIndex;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.StubIdentity;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.StubSearchPort;
import com.campusoverflow.discovery.domain.search.SearchHit;
import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import com.campusoverflow.discovery.domain.search.SearchQuery;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 搜索与推荐的视图组装细节：字段搬运是否忠实、相似度与候选池上限的关系。
 *
 * <p>与 {@link DiscoveryQueryServiceTest} 的分工：那个类验证「查几次、排序对不对」，
 * 这个类验证「搬进视图的每个字段都对得上原始命中」，以及几个容易被改写错的系数。
 */
class DiscoveryQueryServiceViewMappingTest {

    private static final Instant T = DiscoveryTestFixtures.at("2026-10-27T10:00:00Z");

    private StubSearchPort searchPort;
    private RecordingSearchIndex index;
    private StubIdentity identity;
    private DiscoveryQueryService service;

    @BeforeEach
    void setUp() {
        searchPort = new StubSearchPort();
        index = new RecordingSearchIndex();
        identity = new StubIdentity()
                .with(DiscoveryTestFixtures.user(7L, "提问的小张", Role.STUDENT))
                .with(DiscoveryTestFixtures.user(3L, "王老师", Role.TEACHER));
        service = new DiscoveryQueryService(searchPort, index, identity);
    }

    /** 一个把所有布尔/可空字段都取非默认值的命中，用来暴露字段搬错的低级错误。 */
    private static SearchHit richHit() {
        return new SearchHit(21L, "红黑树插入后怎么自平衡", "旋转分四种情况……", List.of("java", "算法", "树"),
                5L, 7L, 42, 7, true, true, T);
    }

    @Nested
    @DisplayName("命中 → 视图的字段搬运")
    class FieldMapping {

        @Test
        @DisplayName("十一个字段逐个对得上，包括可空的 courseId 与两个布尔标记")
        void allFieldsAreCarried() {
            searchPort.result = new PageResult<>(List.of(richHit()), 1, 1, 20);

            SearchResultView v = service.search(new SearchQuery("红黑树", null, null, null, false, false, null))
                    .items().get(0);

            assertThat(v.id()).isEqualTo(21L);
            assertThat(v.title()).isEqualTo("红黑树插入后怎么自平衡");
            assertThat(v.excerpt()).isEqualTo("旋转分四种情况……");
            assertThat(v.tags()).containsExactly("java", "算法", "树");
            assertThat(v.courseId()).isEqualTo(5L);
            assertThat(v.score()).isEqualTo(42);
            assertThat(v.answerCount()).isEqualTo(7);
            assertThat(v.accepted()).isTrue();
            assertThat(v.bountyOpen()).isTrue();
            assertThat(v.createdAt()).isEqualTo(T);
        }

        @Test
        @DisplayName("courseId 为空（非课程问题）时保持为空，不落成 0")
        void nullCourseIdStaysNull() {
            searchPort.result = new PageResult<>(
                    List.of(new SearchHit(22L, "通识问题", "无关课程", List.of(), null, 7L, 0, 0, false, false, T)),
                    1, 1, 20);

            SearchResultView v = service.search(new SearchQuery("通识", null, null, null, false, false, null))
                    .items().get(0);

            // 0 是个合法课程 id，用 0 兜底会让前端把非课程问题显示成「课程 #0」
            assertThat(v.courseId()).isNull();        }

        @Test
        @DisplayName("作者信息带角色名与认证标记，不只搬昵称")
        void authorCarriesRoleAndVerified() {
            identity.with(new com.campusoverflow.identity.api.UserSummary(3L, "王老师", Role.TEACHER, true, "计算机学院"));
            searchPort.result = new PageResult<>(
                    List.of(new SearchHit(23L, "教师提问", "摘要", List.of(), 1L, 3L, 1, 0, false, false, T)),
                    1, 1, 20);

            SearchResultView.Author a = service.search(new SearchQuery("教师", null, null, null, false, false, null))
                    .items().get(0).author();

            assertThat(a.id()).isEqualTo(3L);
            assertThat(a.displayName()).isEqualTo("王老师");
            assertThat(a.role()).isEqualTo("TEACHER");
            assertThat(a.verified()).isTrue();
        }

        @Test
        @DisplayName("已注销用户的占位不含角色，不硬塞一个假角色")
        void unknownAuthorHasNoRole() {
            searchPort.result = new PageResult<>(
                    List.of(new SearchHit(24L, "作者已注销", "摘要", List.of(), null, 999L, 0, 0, false, false, T)),
                    1, 1, 20);

            SearchResultView.Author a = service.search(new SearchQuery("注销", null, null, null, false, false, null))
                    .items().get(0).author();

            assertThat(a.displayName()).isEqualTo("已注销用户");
            assertThat(a.role()).isNull();
            assertThat(a.verified()).isFalse();
        }

        @Test
        @DisplayName("多个作者时按 id 去重后一次查完，同一作者不会查两次")
        void duplicateAuthorsAreLookedUpOnce() {
            searchPort.result = new PageResult<>(
                    List.of(hitWith(31L, 7L), hitWith(32L, 7L), hitWith(33L, 3L)), 3, 1, 20);

            service.search(new SearchQuery("红黑树", null, null, null, false, false, null));

            // 三个命中只有两个不同作者 -> 只发起一次 identity 查询
            assertThat(identity.findUsersCalls).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("相似度与候选池上限")
    class Similarity {

        @Test
        @DisplayName("相似度按本人标签数算分母，不是按候选标签数")
        void denominatorUsesOwnTagCount() {
            // 本人 4 个标签，候选 2 个标签、共享 2 个 -> 2/4 = 0.5（若误用候选数会是 2/2 = 1.0）
            index.tags = List.of("java", "算法", "树", "数据结构");
            index.candidates = List.of(new RelatedCandidate(1L, "A", 2, 2, 0, 0, false));

            var related = service.related(11L, 5);

            assertThat(related).hasSize(1);
            assertThat(related.get(0).similarity()).isCloseTo(0.5, offset(1e-9));
        }

        @Test
        @DisplayName("候选池上限 50 是常量，不受调用方 limit 影响")
        void candidatePoolDoesNotFollowCallerLimit() {
            index.tags = List.of("java");

            service.related(11L, 1);
            int withLimitOne = index.lastCandidateLimit;
            service.related(11L, 50);
            int withLimitFifty = index.lastCandidateLimit;

            // limit 只用于截断最终结果，不能拿去缩候选池——否则相似度排序会失真
            assertThat(withLimitOne).isEqualTo(50);
            assertThat(withLimitFifty).isEqualTo(50);
        }

        @Test
        @DisplayName("limit 超过候选数量时按实际数量返回，不补空")
        void limitAboveCandidateCount() {
            index.tags = List.of("java");
            index.candidates = List.of(new RelatedCandidate(1L, "A", 1, 1, 0, 0, false));

            assertThat(service.related(11L, 10)).hasSize(1);
        }

        @Test
        @DisplayName("负的 limit 与 0 一样直接返回空，不查标签也不查候选")
        void negativeLimitReturnsEmptyEarly() {
            index.tags = List.of("java");
            index.candidates = List.of(new RelatedCandidate(1L, "A", 1, 1, 0, 0, false));

            assertThat(service.related(11L, -3)).isEmpty();
            assertThat(index.lastCandidateLimit).isZero();
        }
    }

    private static SearchHit hitWith(long questionId, long authorId) {
        return new SearchHit(questionId, "问题 " + questionId, "摘要", List.of("java"), 1L, authorId,
                0, 0, false, false, T);
    }
}
