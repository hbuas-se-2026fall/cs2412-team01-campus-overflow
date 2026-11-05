package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.api.CourseSummary;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** QuestionCommandService 用例测试：验证标签计数、事件发布、缓存失效与审计的编排（T1-07）。 */
class QuestionCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private static final long COURSE_ID = 100L;
    private static final long MISSING_COURSE_ID = 404L;
    private static final String BODY = "变色为什么不够？希望有直观解释，最好能给出一个反例。";

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final CountingTags tags = new CountingTags();
    private final RecordingEvents events = new RecordingEvents();
    private final CountingCache cache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final StubIdentity identity = new StubIdentity() {
        @Override
        public Optional<CourseSummary> findCourse(long courseId) {
            return courseId == MISSING_COURSE_ID ? Optional.empty() : super.findCourse(courseId);
        }
    }.withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT).withUser(9, "王老师", Role.TEACHER)
            .withCourseManager(9);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor teacher = new Actor(9, "王老师", Role.TEACHER);

    private QuestionCommandService service;

    @BeforeEach
    void setUp() {
        service = new QuestionCommandService(questions, tags, identity, events, new TruncatingRenderer(), cache,
                audit, clock);
    }

    private long postDefault() {
        return service.post(ming, new PostQuestionCommand(COURSE_ID, "红黑树插入后为什么必须旋转？", BODY,
                List.of("#数据结构", "红黑树", "数据结构")));
    }

    @Test
    @DisplayName("发布：保存问题、标签计数 +1（规范化去重后）、发布事件、失效缓存一次并记录审计")
    void postSavesAndPublishes() {
        long id = postDefault();

        Question saved = questions.findById(id).orElseThrow();
        assertThat(saved.authorId()).isEqualTo(ming.userId());
        assertThat(saved.createdAt()).isEqualTo(NOW);
        assertThat(tags.usage).containsOnly(Map.entry("数据结构", 1), Map.entry("红黑树", 1));

        List<QuestionPostedEvent> posted = events.ofType(QuestionPostedEvent.class);
        assertThat(posted).hasSize(1);
        QuestionPostedEvent e = posted.get(0);
        assertThat(e.questionId()).isEqualTo(id);
        assertThat(e.authorId()).isEqualTo(ming.userId());
        assertThat(e.courseId()).isEqualTo(COURSE_ID);
        assertThat(e.tags()).containsExactly("数据结构", "红黑树");
        assertThat(e.plainText()).isEqualTo("plain:" + QuestionCommandService.PLAIN_TEXT_FOR_INDEX + ":" + BODY);
        assertThat(cache.invalidations).isEqualTo(1);
        assertThat(audit.actions).containsExactly("QUESTION_POSTED");
    }

    @Test
    @DisplayName("发布：不指定课程时不查询课程；指定不存在的课程则 404 且无任何副作用")
    void postValidatesCourse() {
        long id = service.post(ming, new PostQuestionCommand(null, "红黑树插入后为什么必须旋转？", BODY, List.of("java")));
        assertThat(questions.findById(id)).isPresent();

        events.published.clear();
        tags.usage.clear();
        assertThatThrownBy(() -> service.post(ming,
                new PostQuestionCommand(MISSING_COURSE_ID, "红黑树插入后为什么必须旋转？", BODY, List.of("java"))))
                .isInstanceOf(NotFoundException.class);
        assertThat(events.published).isEmpty();
        assertThat(tags.usage).isEmpty();
    }

    @Test
    @DisplayName("发布：标题过短由领域值对象拒绝，不落库也不发事件")
    void postRejectsInvalidTitle() {
        assertThatThrownBy(() -> service.post(ming, new PostQuestionCommand(COURSE_ID, "太短的标题", BODY, List.of("java"))))
                .isInstanceOf(DomainException.class);
        assertThat(questions.findById(1)).isEmpty();
        assertThat(events.published).isEmpty();
        assertThat(cache.invalidations).isZero();
    }

    @Test
    @DisplayName("编辑：只按新旧标签差集调整计数，发布 QuestionEdited 并失效缓存")
    void editAdjustsTagDiffOnly() {
        long id = postDefault();
        tags.usage.clear();
        events.published.clear();

        service.edit(ming, id, new EditQuestionCommand("红黑树插入后为什么一定要旋转？", BODY, List.of("红黑树", "算法")));

        assertThat(tags.usage).containsOnly(Map.entry("算法", 1), Map.entry("数据结构", -1));
        List<QuestionEditedEvent> edited = events.ofType(QuestionEditedEvent.class);
        assertThat(edited).hasSize(1);
        assertThat(edited.get(0).title()).isEqualTo("红黑树插入后为什么一定要旋转？");
        assertThat(edited.get(0).tags()).containsExactly("红黑树", "算法");
        assertThat(questions.findById(id).orElseThrow().updatedAt()).isEqualTo(NOW);
        assertThat(cache.invalidations).isEqualTo(2);
        assertThat(audit.actions).containsExactly("QUESTION_POSTED", "QUESTION_EDITED");
    }

    @Test
    @DisplayName("编辑：标签不变时不调整计数")
    void editWithSameTagsKeepsUsage() {
        long id = postDefault();
        tags.usage.clear();

        service.edit(ming, id, new EditQuestionCommand("红黑树插入后为什么一定要旋转？", BODY, List.of("红黑树", "数据结构")));

        assertThat(tags.calls).isEqualTo(1);
    }

    @Test
    @DisplayName("编辑：非作者非管理者 403；课程教师可以编辑他人问题")
    void editPermission() {
        long id = postDefault();
        events.published.clear();

        assertThatThrownBy(() -> service.edit(hong, id,
                new EditQuestionCommand("红黑树插入后为什么一定要旋转？", BODY, List.of("红黑树"))))
                .isInstanceOf(ForbiddenException.class);
        assertThat(events.published).isEmpty();

        service.edit(teacher, id, new EditQuestionCommand("红黑树插入后为什么一定要旋转？", BODY, List.of("红黑树")));
        assertThat(events.ofType(QuestionEditedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("编辑/删除：问题不存在统一 404 QA-4001")
    void missingQuestionIs404() {
        assertThatThrownBy(() -> service.edit(ming, 999,
                new EditQuestionCommand("红黑树插入后为什么一定要旋转？", BODY, List.of("红黑树"))))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((DomainException) ex).code()).isEqualTo("QA-4001");
        assertThatThrownBy(() -> service.delete(ming, 999))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((DomainException) ex).code()).isEqualTo("QA-4001");
    }

    @Test
    @DisplayName("删除：软删除、标签计数 -1、发布 ContentDeleted(QUESTION) 并记录审计 QUESTION_DELETED")
    void deletePublishesContentDeleted() {
        long id = postDefault();
        tags.usage.clear();

        service.delete(ming, id);

        assertThat(questions.findById(id).orElseThrow().isDeleted()).isTrue();
        assertThat(tags.usage).containsOnly(Map.entry("数据结构", -1), Map.entry("红黑树", -1));
        List<ContentDeletedEvent> deleted = events.ofType(ContentDeletedEvent.class);
        assertThat(deleted).hasSize(1);
        assertThat(deleted.get(0).targetType()).isEqualTo("QUESTION");
        assertThat(deleted.get(0).targetId()).isEqualTo(id);
        assertThat(deleted.get(0).questionId()).isEqualTo(id);
        assertThat(cache.invalidations).isEqualTo(2);
        assertThat(audit.actions).containsExactly("QUESTION_POSTED", "QUESTION_DELETED");
    }

    @Test
    @DisplayName("删除：作者不能删已有回答的问题，课程教师可以；已删除的问题不能再编辑或删除")
    void deleteRules() {
        long id = postDefault();
        Question q = questions.findById(id).orElseThrow();
        questions.save(Question.reconstitute(q.id(), q.authorId(), q.courseId(), q.title(), q.body(), q.tags(),
                null, 0, 1, 0, q.createdAt(), q.updatedAt(), null, q.version()));

        assertThatThrownBy(() -> service.delete(ming, id)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.delete(hong, id)).isInstanceOf(ForbiddenException.class);
        assertThat(events.ofType(ContentDeletedEvent.class)).isEmpty();

        service.delete(teacher, id);
        assertThat(events.ofType(ContentDeletedEvent.class)).hasSize(1);

        assertThatThrownBy(() -> service.delete(teacher, id)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.edit(teacher, id,
                new EditQuestionCommand("红黑树插入后为什么一定要旋转？", BODY, List.of("红黑树"))))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(events.ofType(ContentDeletedEvent.class)).hasSize(1);
    }

    /** 记录每个标签的累计计数变化。 */
    static class CountingTags implements TagRepository {
        final Map<String, Integer> usage = new HashMap<>();
        int calls;

        @Override
        public void adjustUsage(Collection<String> tagNames, int delta) {
            calls++;
            tagNames.forEach(name -> usage.merge(name, delta, Integer::sum));
        }
    }

    /** 可断言的渲染替身：把长度上限编码进输出，验证服务传入了 PLAIN_TEXT_FOR_INDEX。 */
    static class TruncatingRenderer implements ContentRenderer {
        @Override
        public String toSafeHtml(String markdown) {
            return markdown;
        }

        @Override
        public String toPlainText(String markdown, int maxLength) {
            return "plain:" + maxLength + ":" + markdown;
        }
    }
}
