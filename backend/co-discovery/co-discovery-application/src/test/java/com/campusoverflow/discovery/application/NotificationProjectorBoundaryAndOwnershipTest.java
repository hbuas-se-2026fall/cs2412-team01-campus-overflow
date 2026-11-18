package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.InMemoryNotifications;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.StubIdentity;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.StubQa;
import com.campusoverflow.discovery.domain.notification.Notification;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 通知中心的边界行为：已知接收者的「不该发」分支、下游缺失时的兜底文案、
 * 同一事件对多个接收者的幂等键，以及通知对象的归属校验。
 *
 * <p>与 {@link NotificationProjectorTest} 的分工：那个类盯每条通知的正文内容，
 * 这个类盯「什么时候不该产生通知」和「旧通知能被谁改」。
 */
class NotificationProjectorBoundaryAndOwnershipTest {

    private static final Instant T = DiscoveryTestFixtures.at("2026-10-27T10:00:00Z");

    private static final long ASKER = 7L;
    private static final long ANSWERER = 9L;
    private static final long NOBODY_NAME_MIN = 108L;

    private InMemoryNotifications notifications;
    private StubIdentity identity;
    private StubQa qa;
    private NotificationProjector projector;

    @BeforeEach
    void setUp() {
        notifications = new InMemoryNotifications();
        identity = new StubIdentity()
                .with(DiscoveryTestFixtures.user(ASKER, "提问的小张", Role.STUDENT))
                .with(DiscoveryTestFixtures.user(ANSWERER, "回答的小李", Role.STUDENT))
                .with(DiscoveryTestFixtures.user(3L, "王老师", Role.TEACHER))
                .with(DiscoveryTestFixtures.user(4L, "小明", Role.STUDENT));
        qa = new StubQa().with(new QuestionRef(11L, ASKER, 1L, "红黑树怎么旋转", null, false));
        projector = new NotificationProjector(notifications, identity, qa);
    }

    @Nested
    @DisplayName("不该发通知的四种自相关情形")
    class SilentPaths {

        @Test
        @DisplayName("问题作者采纳自己的回答：不发通知")
        void questionAuthorAcceptsOwnAnswer() {
            projector.on(new AnswerAcceptedEvent("b1", T, 5L, 11L, ASKER, ASKER, ASKER, "红黑树怎么旋转"));

            assertThat(notifications.saved).isEmpty();
        }

        @Test
        @DisplayName("回答作者认证自己的回答：不发通知")
        void selfEndorsement() {
            projector.on(new AnswerEndorsedEvent("b2", T, 5L, 11L, ANSWERER, ANSWERER, "红黑树怎么旋转"));

            assertThat(notifications.saved).isEmpty();
        }

        @Test
        @DisplayName("赞助人自己拿走悬赏：不发通知")
        void sponsorWinsOwnBounty() {
            projector.on(new BountyAwardedEvent("b3", T, 1L, 11L, ASKER, ASKER, 80));

            assertThat(notifications.saved).isEmpty();
        }

        @Test
        @DisplayName("评论里没有任何 @：不发通知，也不去查询被提及者")
        void commentWithoutMention() {
            projector.on(new CommentPostedEvent("b4", T, 2L, 11L, ANSWERER, "这个我也想知道"));

            assertThat(notifications.saved).isEmpty();
        }
    }

    @Nested
    @DisplayName("昵称、标题查不到时的兜底文案")
    class FallbackText {

        @Test
        @DisplayName("答主已注销：昵称退化为「某位用户」，通知照发")
        void unknownAnswerAuthorFallsBackToGenericName() {
            projector.on(new AnswerSubmittedEvent("b5", T, 5L, 11L, NOBODY_NAME_MIN, ASKER, "红黑树怎么旋转"));

            assertThat(notifications.only().title()).startsWith("某位用户").contains("红黑树怎么旋转");
        }

        @Test
        @DisplayName("认证人查不到：同样退化为「某位用户」，不因查不到就丢掉通知")
        void unknownEndorserFallsBackToGenericName() {
            projector.on(new AnswerEndorsedEvent("b6", T, 5L, 11L, ANSWERER, NOBODY_NAME_MIN, "红黑树怎么旋转"));

            assertThat(notifications.only().title()).startsWith("某位用户");
        }

        @Test
        @DisplayName("被评论的问题已不在问答上下文：标题退化为「问题 #id」")
        void unknownQuestionFallsBackToIdTitle() {
            projector.on(new CommentPostedEvent("b7", T, 2L, 99L, ANSWERER, "帮顶 @王老师"));

            assertThat(notifications.only().title()).contains("问题 #99").doesNotContain("红黑树");
            assertThat(notifications.only().link()).isEqualTo("/questions/99");
        }
    }

    @Nested
    @DisplayName("提及通知的幂等与落空路径")
    class MentionEdgeCases {

        @Test
        @DisplayName("@ 了不存在的昵称：跳过，不发通知")
        void mentionOfUnknownUserIsSkipped() {
            projector.on(new CommentPostedEvent("b8", T, 2L, 11L, ANSWERER, "请问 @助教甲 在吗"));

            assertThat(notifications.saved).isEmpty();
        }

        @Test
        @DisplayName("同一事件里 @ 了同一人两次：按事件+接收者去重，只落一条")
        void sameEventMentioningSameUserTwice() {
            projector.on(new CommentPostedEvent("b9", T, 2L, 11L, ANSWERER, "@王老师 @王老师 麻烦看看"));

            assertThat(notifications.saved).hasSize(1);
            assertThat(notifications.only().recipientId()).isEqualTo(3L);
        }
    }

    @Test
    @DisplayName("INV-N4 的键是（事件, 接收者）：同一事件对甲重复投递被挡，对乙仍要新发")
    void duplicateGuardIsKeyedByRecipientNotByEventAlone() {
        // 同一 eventId 已经给王老师发过一条
        notifications.save(Notification.create(3L, NotificationType.MENTIONED, "旧的一条", "/questions/11",
                "b10", T));
        int before = notifications.saved.size();

        projector.on(new CommentPostedEvent("b10", T, 2L, 11L, ANSWERER, "@王老师 @小明 看看"));

        // 王老师被挡掉，小明收到一条 —— 若按事件整体去重，小明这条就会丢
        assertThat(notifications.saved).hasSize(before + 1);
        assertThat(notifications.saved).extracting(Notification::recipientId).containsExactly(3L, 4L);
    }

    @Nested
    @DisplayName("通知归属：只有接收者本人能标记已读")
    class Ownership {

        private NotificationService service;
        private Notification mine;

        @BeforeEach
        void seed() {
            service = new NotificationService(notifications);
            mine = notifications.save(Notification.create(ASKER, NotificationType.ANSWER_RECEIVED,
                    "回答的小李 回答了你的问题「红黑树怎么旋转」", "/questions/11", "o1", T));
        }

        @Test
        @DisplayName("本人标记已读：成功，且落库后仍是同一个通知")
        void ownerMarksRead() {
            service.markRead(new Actor(ASKER, "提问的小张", Role.STUDENT), mine.id());

            assertThat(notifications.findById(mine.id())).get().extracting(Notification::read).isEqualTo(true);
        }

        @Test
        @DisplayName("别人来标记已读：抛 DIS-3001，状态不变")
        void strangerCannotMarkRead() {
            // 错误码在 code() 上，异常 message 只有中文说明，所以断言要分开写
            assertThatThrownBy(() -> service.markRead(new Actor(ANSWERER, "回答的小李", Role.STUDENT), mine.id()))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(e -> assertThat(((ForbiddenException) e).code()).isEqualTo("DIS-3001"))
                    .hasMessageContaining("只能标记自己的通知为已读");

            assertThat(notifications.findById(mine.id())).get().extracting(Notification::read).isEqualTo(false);
        }

        @Test
        @DisplayName("重复标记已读是幂等的：不报错，只落一条记录且已读")
        void markingReadTwiceIsIdempotent() {
            Actor owner = new Actor(ASKER, "提问的小张", Role.STUDENT);
            service.markRead(owner, mine.id());
            service.markRead(owner, mine.id());

            // 测试替身的 save 是「写入流水」而非 upsert，同一条会被记两次，
            // 所以这里断言去重后的对象数（按 id）而不是 saved 的条数
            assertThat(notifications.saved).extracting(Notification::id).containsOnly(mine.id());
            assertThat(notifications.findById(mine.id())).get().extracting(Notification::read).isEqualTo(true);
        }

        @Test
        @DisplayName("列表按接收者过滤：别人的通知不会混进来")
        void listOnlyReturnsOwnNotifications() {
            notifications.save(Notification.create(ANSWERER, NotificationType.BADGE_AWARDED, "获得新徽章：首答",
                    "/me/badges", "o2", T));

            PageResult<NotificationView> minePage = service.list(new Actor(ASKER, "提问的小张", Role.STUDENT),
                    new PageRequest(1, 20));

            assertThat(minePage.total()).isEqualTo(1);
            assertThat(minePage.items()).extracting(NotificationView::type).containsExactly("ANSWER_RECEIVED");
        }
    }
}
