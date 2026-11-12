package com.campusoverflow.qa.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 问答查询用例测试：基于假读模型，验证缓存旁路、批量取用户、回答排序、评论分组与按钮权限提示。 */
class QuestionQueryServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-08T06:00:00Z");

    private final FakeReadModel readModel = new FakeReadModel();
    private final RecordingViewCounter viewCounter = new RecordingViewCounter();
    private final MapCache cache = new MapCache();
    private StubIdentity identity;

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor gang = new Actor(3, "小刚", Role.STUDENT);
    private final Actor wang = new Actor(9, "王老师", Role.TEACHER);

    private QuestionQueryService service;

    @BeforeEach
    void setUp() {
        identity = new StubIdentity()
                .withUser(1, "小明", Role.STUDENT)
                .withUser(2, "小红", Role.STUDENT)
                .withUser(3, "小刚", Role.STUDENT)
                .withUser(9, "王老师", Role.TEACHER)
                .withCourseManager(9);
        service = new QuestionQueryService(readModel, identity, new FakeRenderer(), viewCounter, cache);
    }

    private static QuestionRow question(long id, long authorId, Long acceptedAnswerId, int answerCount) {
        return new QuestionRow(id, authorId, 100L, "红黑树插入后为什么必须旋转？", "**变色**为什么不够？",
                List.of("数据结构", "红黑树"), acceptedAnswerId, 3, answerCount, 7, T0, T0);
    }

    private static AnswerRow answer(long id, long authorId, int score, int minutesLater, Long endorsedBy) {
        Instant at = T0.plusSeconds(60L * minutesLater);
        return new AnswerRow(id, authorId, "回答" + id, score, endorsedBy, at, at);
    }

    // ---------------------------------------------------------------- list

    @Test
    @DisplayName("列表：摘要为纯文本、accepted 由 acceptedAnswerId 推出、作者信息批量回填，并写入缓存")
    void listMapsRowsAndFillsCache() {
        readModel.questions.put(10L, question(10, 1, 50L, 2));
        readModel.questions.put(11L, question(11, 2, null, 0));
        PageRequest page = new PageRequest(0, 20);

        PageResult<QuestionSummaryView> result = service.list(100L, page);

        assertThat(result.items()).hasSize(2);
        QuestionSummaryView first = result.items().get(0);
        assertThat(first.excerpt()).isEqualTo("plain[160]:**变色**为什么不够？");
        assertThat(first.accepted()).isTrue();
        assertThat(first.author().displayName()).isEqualTo("小明");
        assertThat(result.items().get(1).accepted()).isFalse();
        assertThat(cache.store).containsKey(key(100L, page));
        assertThat(readModel.listCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("列表：缓存命中时直接返回，不访问读模型")
    void listHitsCache() {
        PageRequest page = new PageRequest(0, 20);
        PageResult<QuestionSummaryView> cached = PageResult.empty(0, 20);
        cache.store.put(key(null, page), cached);

        assertThat(service.list(null, page)).isSameAs(cached);
        assertThat(readModel.listCalls).isZero();
    }

    @Test
    @DisplayName("列表：作者已注销时显示占位名而不是报错")
    void listToleratesMissingAuthor() {
        readModel.questions.put(10L, question(10, 404, null, 0));

        QuestionSummaryView view = service.list(null, new PageRequest(0, 20)).items().get(0);

        assertThat(view.author().id()).isEqualTo(404);
        assertThat(view.author().displayName()).isEqualTo("已注销用户");
    }

    // ---------------------------------------------------------------- detail

    @Test
    @DisplayName("详情：问题不存在 404（QA-4001），且不计浏览量")
    void detailOfMissingQuestionIsNotFound() {
        assertThatThrownBy(() -> service.detail(999L, ming))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo("QA-4001");
        assertThat(viewCounter.increments).isEmpty();
    }

    @Test
    @DisplayName("详情：浏览量 +1，正文渲染为安全 HTML，课程信息回填")
    void detailIncrementsViewsAndRenders() {
        readModel.questions.put(10L, question(10, 1, null, 0));

        QuestionDetailView view = service.detail(10L, null);

        assertThat(viewCounter.increments).containsExactly(10L);
        assertThat(view.bodyHtml()).isEqualTo("html:**变色**为什么不够？");
        assertThat(view.course().code()).isEqualTo("CS2001");
        assertThat(view.author().displayName()).isEqualTo("小明");
    }

    @Test
    @DisplayName("详情：回答排序——已采纳置顶，其后得分降序，同分按时间升序")
    void answersAreOrderedAcceptedThenScoreThenTime() {
        readModel.questions.put(10L, question(10, 1, 53L, 4));
        readModel.answers.put(10L, List.of(
                answer(51, 2, 5, 3, null),
                answer(52, 3, 5, 1, null),
                answer(53, 2, 0, 0, null),
                answer(54, 3, 9, 2, null)));

        List<AnswerView> answers = service.detail(10L, null).answers();

        assertThat(answers).extracting(AnswerView::id).containsExactly(53L, 54L, 52L, 51L);
        assertThat(answers.get(0).accepted()).isTrue();
        assertThat(answers.subList(1, 4)).noneMatch(AnswerView::accepted);
    }

    @Test
    @DisplayName("详情：所有涉及的用户（提问者、回答者、认证教师、评论者）一次批量查询")
    void usersAreFetchedInOneBatch() {
        CountingIdentity counting = new CountingIdentity();
        service = new QuestionQueryService(readModel, counting, new FakeRenderer(), viewCounter, cache);
        readModel.questions.put(10L, question(10, 1, null, 1));
        readModel.answers.put(10L, List.of(answer(50, 2, 0, 0, 9L)));
        readModel.comments.put(10L, List.of(new CommentRow(70, "ANSWER", 50, 3, null, "黑高是什么？", T0)));

        AnswerView answer = service.detail(10L, null).answers().get(0);

        assertThat(counting.findUsersCalls).isEqualTo(1);
        assertThat(counting.lastRequested).containsExactlyInAnyOrder(1L, 2L, 3L, 9L);
        assertThat(answer.endorsedBy().displayName()).isEqualTo("王老师");
    }

    @Test
    @DisplayName("详情：评论按目标分组——问题评论挂在问题下，回答评论挂在对应回答下")
    void commentsAreGroupedByTarget() {
        readModel.questions.put(10L, question(10, 1, null, 2));
        readModel.answers.put(10L, List.of(answer(50, 2, 1, 0, null), answer(51, 3, 0, 1, null)));
        readModel.comments.put(10L, List.of(
                new CommentRow(70, "QUESTION", 10, 3, null, "题目再具体点？", T0),
                new CommentRow(71, "ANSWER", 50, 1, null, "黑高是什么？", T0),
                new CommentRow(72, "ANSWER", 50, 2, 71L, "根到叶子的黑节点数", T0)));

        QuestionDetailView view = service.detail(10L, null);

        assertThat(view.comments()).extracting(CommentView::id).containsExactly(70L);
        assertThat(view.answers().get(0).comments()).extracting(CommentView::id).containsExactly(71L, 72L);
        assertThat(view.answers().get(1).comments()).isEmpty();
    }

    @Test
    @DisplayName("详情：登录后回填我的投票（键形如 ANSWER:50），未登录不查询投票")
    void myVotesAreFilledOnlyWhenLoggedIn() {
        readModel.questions.put(10L, question(10, 1, null, 1));
        readModel.answers.put(10L, List.of(answer(50, 2, 1, 0, null)));
        readModel.votes.put("QUESTION:10", 1);
        readModel.votes.put("ANSWER:50", -1);

        QuestionDetailView anonymous = service.detail(10L, null);
        assertThat(anonymous.myVote()).isZero();
        assertThat(anonymous.answers().get(0).myVote()).isZero();
        assertThat(readModel.myVotesCalls).isZero();

        QuestionDetailView mine = service.detail(10L, gang);
        assertThat(mine.myVote()).isEqualTo(1);
        assertThat(mine.answers().get(0).myVote()).isEqualTo(-1);
        assertThat(readModel.lastVoterId).isEqualTo(gang.userId());
    }

    @Test
    @DisplayName("权限提示：未登录无任何按钮")
    void anonymousHasNoPermissions() {
        readModel.questions.put(10L, question(10, 1, null, 0));

        assertThat(service.detail(10L, null).permissions()).isEqualTo(PermissionsView.NONE);
    }

    @Test
    @DisplayName("权限提示：作者在无回答时可编辑、删除、采纳；有回答后不可删除；已采纳后不可再采纳")
    void authorPermissionsFollowAggregateRules() {
        readModel.questions.put(10L, question(10, 1, null, 0));
        assertThat(service.detail(10L, ming).permissions())
                .isEqualTo(new PermissionsView(true, true, true, false));

        readModel.questions.put(10L, question(10, 1, null, 2));
        assertThat(service.detail(10L, ming).permissions())
                .isEqualTo(new PermissionsView(true, false, true, false));

        readModel.questions.put(10L, question(10, 1, 50L, 2));
        assertThat(service.detail(10L, ming).permissions())
                .isEqualTo(new PermissionsView(true, false, false, false));
    }

    @Test
    @DisplayName("权限提示：课程教师可删除有回答的问题，并能编辑任意回答；其他学生没有任何权限")
    void managerAndStrangerPermissions() {
        readModel.questions.put(10L, question(10, 1, null, 1));
        readModel.answers.put(10L, List.of(answer(50, 2, 0, 0, null)));

        QuestionDetailView asTeacher = service.detail(10L, wang);
        assertThat(asTeacher.permissions()).isEqualTo(new PermissionsView(true, true, true, true));
        assertThat(asTeacher.answers().get(0).canEdit()).isTrue();

        QuestionDetailView asStranger = service.detail(10L, gang);
        assertThat(asStranger.permissions()).isEqualTo(new PermissionsView(false, false, false, false));
        assertThat(asStranger.answers().get(0).canEdit()).isFalse();

        assertThat(service.detail(10L, hong).answers().get(0).canEdit()).isTrue();
    }

    // ---------------------------------------------------------------- popularTags

    @Test
    @DisplayName("热门标签：limit 被限制在 1–50")
    void popularTagsLimitIsClamped() {
        service.popularTags(0);
        assertThat(readModel.lastTagLimit).isEqualTo(1);
        service.popularTags(500);
        assertThat(readModel.lastTagLimit).isEqualTo(PageRequest.MAX_SIZE);
        service.popularTags(10);
        assertThat(readModel.lastTagLimit).isEqualTo(10);
    }

    // ---------------------------------------------------------------- 测试替身

    private static String key(Long courseId, PageRequest page) {
        return courseId + ":" + page.page() + ":" + page.size();
    }

    static class FakeReadModel implements QaReadModel {
        final Map<Long, QuestionRow> questions = new LinkedHashMap<>();
        final Map<Long, List<AnswerRow>> answers = new HashMap<>();
        final Map<Long, List<CommentRow>> comments = new HashMap<>();
        final Map<String, Integer> votes = new HashMap<>();
        int listCalls;
        int myVotesCalls;
        Long lastVoterId;
        int lastTagLimit;

        @Override
        public PageResult<QuestionRow> listQuestions(Long courseId, PageRequest page) {
            listCalls++;
            List<QuestionRow> items = new ArrayList<>(questions.values());
            return new PageResult<>(items, items.size(), page.page(), page.size());
        }

        @Override
        public Optional<QuestionRow> findQuestion(long questionId) {
            return Optional.ofNullable(questions.get(questionId));
        }

        @Override
        public List<AnswerRow> findAnswers(long questionId) {
            return answers.getOrDefault(questionId, List.of());
        }

        @Override
        public List<CommentRow> findComments(long questionId) {
            return comments.getOrDefault(questionId, List.of());
        }

        @Override
        public Map<String, Integer> findMyVotes(long voterId, long questionId, Collection<Long> answerIds) {
            myVotesCalls++;
            lastVoterId = voterId;
            return votes;
        }

        @Override
        public List<TagView> popularTags(int limit) {
            lastTagLimit = limit;
            return List.of(new TagView("java", 3));
        }
    }

    static class FakeRenderer implements ContentRenderer {
        @Override
        public String toSafeHtml(String markdown) {
            return "html:" + markdown;
        }

        @Override
        public String toPlainText(String markdown, int maxLength) {
            return "plain[" + maxLength + "]:" + markdown;
        }
    }

    static class RecordingViewCounter implements ViewCounter {
        final List<Long> increments = new ArrayList<>();

        @Override
        public void increment(long questionId) {
            increments.add(questionId);
        }
    }

    static class MapCache implements QuestionListCache {
        final Map<String, PageResult<QuestionSummaryView>> store = new HashMap<>();

        @Override
        public Optional<PageResult<QuestionSummaryView>> get(Long courseId, PageRequest page) {
            return Optional.ofNullable(store.get(key(courseId, page)));
        }

        @Override
        public void put(Long courseId, PageRequest page, PageResult<QuestionSummaryView> value) {
            store.put(key(courseId, page), value);
        }

        @Override
        public void invalidateAll() {
            store.clear();
        }
    }

    static class CountingIdentity extends StubIdentity {
        int findUsersCalls;
        List<Long> lastRequested = List.of();

        CountingIdentity() {
            withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT)
                    .withUser(3, "小刚", Role.STUDENT).withUser(9, "王老师", Role.TEACHER);
        }

        @Override
        public Map<Long, com.campusoverflow.identity.api.UserSummary> findUsers(Collection<Long> userIds) {
            findUsersCalls++;
            lastRequested = List.copyOf(userIds);
            return super.findUsers(userIds);
        }
    }
}
