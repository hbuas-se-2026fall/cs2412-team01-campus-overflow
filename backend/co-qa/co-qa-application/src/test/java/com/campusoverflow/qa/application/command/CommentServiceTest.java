package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryComments;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 评论用例测试：验证目标解析、父评论加载与事件发布；一层回复等领域规则只验证其被正确触发。 */
class CommentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryComments comments = new InMemoryComments();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor gang = new Actor(3, "小刚", Role.STUDENT);

    private CommentService service;
    private Question question;
    private Answer answer;

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, questions, answers, events, clock);
        question = questions.save(Question.post(ming.userId(), 100L, new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释，最好能给出一个反例。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW));
        answer = answers.save(Answer.submit(question, hong.userId(),
                MarkdownBody.ofAnswer("关键在于黑高这个不变量，仅靠变色无法调整。"), NOW));
    }

    @Test
    @DisplayName("评论问题：保存评论并发布携带正文的 CommentPosted 事件（供 Discovery 解析 @提及）")
    void commentOnQuestionPublishesEventWithBody() {
        long id = service.post(gang, new PostCommentCommand(TargetType.QUESTION, question.id(), null, "@小明 能补充一下代码吗？"));

        Comment saved = comments.findById(id).orElseThrow();
        assertThat(saved.questionId()).isEqualTo(question.id());
        assertThat(saved.authorId()).isEqualTo(gang.userId());
        assertThat(saved.parentId()).isNull();

        List<CommentPostedEvent> posted = events.ofType(CommentPostedEvent.class);
        assertThat(posted).hasSize(1);
        assertThat(posted.get(0).commentId()).isEqualTo(id);
        assertThat(posted.get(0).questionId()).isEqualTo(question.id());
        assertThat(posted.get(0).authorId()).isEqualTo(gang.userId());
        assertThat(posted.get(0).body()).isEqualTo("@小明 能补充一下代码吗？");
    }

    @Test
    @DisplayName("评论回答：questionId 解析为回答所属的问题")
    void commentOnAnswerResolvesOwningQuestion() {
        long id = service.post(ming, new PostCommentCommand(TargetType.ANSWER, answer.id(), null, "谢谢，讲得很清楚"));

        assertThat(comments.findById(id).orElseThrow().questionId()).isEqualTo(question.id());
        assertThat(events.ofType(CommentPostedEvent.class).get(0).questionId()).isEqualTo(question.id());
    }

    @Test
    @DisplayName("一层回复：回复顶层评论成功；再回复这条回复被拒绝（QA-1007）且不发事件")
    void replyIsLimitedToOneLevel() {
        long top = service.post(gang, new PostCommentCommand(TargetType.ANSWER, answer.id(), null, "黑高是什么？"));
        long reply = service.post(hong, new PostCommentCommand(TargetType.ANSWER, answer.id(), top, "根到叶子的黑节点数"));

        assertThat(comments.findById(reply).orElseThrow().parentId()).isEqualTo(top);

        assertThatThrownBy(() -> service.post(gang,
                new PostCommentCommand(TargetType.ANSWER, answer.id(), reply, "明白了，谢谢")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("QA-1007");
        assertThat(events.ofType(CommentPostedEvent.class)).hasSize(2);
    }

    @Test
    @DisplayName("回复必须与父评论挂在同一内容下（QA-1012）")
    void replyMustTargetSameContentAsParent() {
        long top = service.post(gang, new PostCommentCommand(TargetType.QUESTION, question.id(), null, "题目描述再具体点？"));

        assertThatThrownBy(() -> service.post(hong,
                new PostCommentCommand(TargetType.ANSWER, answer.id(), top, "我在回答下回复你")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("QA-1012");
    }

    @Test
    @DisplayName("父评论不存在：404（QA-4004）")
    void missingParentIsNotFound() {
        assertThatThrownBy(() -> service.post(gang,
                new PostCommentCommand(TargetType.QUESTION, question.id(), 999L, "回复一条不存在的评论")))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo("QA-4004");
    }

    @Test
    @DisplayName("目标不存在：问题 404（QA-4001）、回答 404（QA-4002）")
    void missingTargetIsNotFound() {
        assertThatThrownBy(() -> service.post(gang, new PostCommentCommand(TargetType.QUESTION, 999L, null, "你好")))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo("QA-4001");
        assertThatThrownBy(() -> service.post(gang, new PostCommentCommand(TargetType.ANSWER, 999L, null, "你好")))
                .isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo("QA-4002");
    }

    @Test
    @DisplayName("已删除的回答不能再评论（QA-1008），且不发事件")
    void cannotCommentOnDeletedAnswer() {
        answer.softDelete(hong, false, false, NOW);

        assertThatThrownBy(() -> service.post(gang, new PostCommentCommand(TargetType.ANSWER, answer.id(), null, "还在吗")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("QA-1008");
        assertThat(events.published).isEmpty();
    }

    @Test
    @DisplayName("正文超过 600 字符被拒绝（QA-1002）")
    void bodyTooLongIsRejected() {
        String tooLong = "啊".repeat(Comment.MAX_LENGTH + 1);

        assertThatThrownBy(() -> service.post(gang, new PostCommentCommand(TargetType.QUESTION, question.id(), null, tooLong)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("QA-1002");
    }
}
