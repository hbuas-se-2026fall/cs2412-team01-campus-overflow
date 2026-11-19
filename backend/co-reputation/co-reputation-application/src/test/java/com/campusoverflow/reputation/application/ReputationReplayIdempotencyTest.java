package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.application.fake.ReputationHarness;
import com.campusoverflow.reputation.domain.BadgeAward;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.Bounty;
import com.campusoverflow.reputation.domain.BountyStatus;
import com.campusoverflow.reputation.domain.LedgerEntry;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 事件重放（outbox_event.published_at 置空后被再次投递）的幂等性：
 * 声誉、流水、徽章、悬赏都不得重复变化。对应指导书 6.4.3 的「验收重点」。
 */
class ReputationReplayIdempotencyTest {

    private static final Instant NOW = Instant.parse("2026-10-27T08:00:00Z");
    private static final long SPONSOR = 9L;
    private static final long AUTHOR = 100L;
    private static final long ASKER = 200L;

    private ReputationHarness harness() {
        return new ReputationHarness(NOW.toString());
    }

    private static UserRegisteredEvent registered(String eventId, long userId, Role role) {
        return new UserRegisteredEvent(eventId, NOW, userId, "用户" + userId, role);
    }

    private static AnswerAcceptedEvent accepted(String eventId) {
        return new AnswerAcceptedEvent(eventId, NOW, 21L, 7L, AUTHOR, ASKER, ASKER, "如何理解领域事件");
    }

    private static AnswerSubmittedEvent submitted(String eventId) {
        return new AnswerSubmittedEvent(eventId, NOW, 21L, 7L, AUTHOR, ASKER, "如何理解领域事件");
    }

    private static AnswerEndorsedEvent endorsed(String eventId) {
        return new AnswerEndorsedEvent(eventId, NOW, 21L, 7L, AUTHOR, SPONSOR, "如何理解领域事件");
    }

    private static VoteCastEvent upvote(String eventId) {
        return new VoteCastEvent(eventId, NOW, "ANSWER", 21L, AUTHOR, 300L, 7L, 0, 1);
    }

    @Test
    @DisplayName("采纳事件重放：仍只 +15、一条流水、一枚「初露锋芒」")
    void acceptedReplayCreditsOnce() {
        ReputationHarness h = harness();
        AnswerAcceptedEvent e = accepted("evt-a1");

        h.handlers.on(e);
        h.handlers.on(e);

        assertThat(h.accounts.get(AUTHOR).reputation()).isEqualTo(15);
        assertThat(h.ledger.entries()).extracting(LedgerEntry::reason)
                .containsExactly(ReputationReason.ANSWER_ACCEPTED);
        assertThat(h.events.ofType(BadgeAwardedEvent.class)).extracting(BadgeAwardedEvent::badgeCode)
                .containsExactly(BadgeType.FIRST_ACCEPTED.name());
    }

    @Test
    @DisplayName("采纳事件重放不会二次结算悬赏：获奖者只 +50，发起人冻结归零后不再变化")
    void acceptedReplaySettlesBountyOnce() {
        ReputationHarness h = harness();
        Bounty bounty = Bounty.reconstitute(1L, 7L, SPONSOR, 50, BountyStatus.OPEN,
                NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(6)), null, null, null);
        h.bounties.with(bounty);
        h.accounts.with(ReputationAccount.reconstitute(SPONSOR, 0, 950, 50, 0L));
        AnswerAcceptedEvent e = accepted("evt-a1");

        h.handlers.on(e);
        h.handlers.on(e);

        assertThat(bounty.status()).isEqualTo(BountyStatus.AWARDED);
        assertThat(h.accounts.get(SPONSOR).availablePoints()).isEqualTo(950);
        assertThat(h.accounts.get(SPONSOR).frozenPoints()).isZero();
        assertThat(h.accounts.get(AUTHOR).reputation()).isEqualTo(15 + 50);
        assertThat(h.ledger.entries()).filteredOn(x -> x.reason() == ReputationReason.BOUNTY_AWARDED)
                .hasSize(1);
        assertThat(h.events.ofType(BountyAwardedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("注册事件在入账之后重放，不会把账户重置为初始状态")
    void registrationReplayDoesNotResetAccount() {
        ReputationHarness h = harness();
        h.handlers.on(registered("evt-r1", AUTHOR, Role.STUDENT));
        h.handlers.on(accepted("evt-a1"));

        h.handlers.on(registered("evt-r1", AUTHOR, Role.STUDENT));

        assertThat(h.accounts.get(AUTHOR).reputation()).isEqualTo(15);
    }

    @Test
    @DisplayName("注册事件重放时角色不同，也不会改写初始积分")
    void registrationReplayIgnoresRoleChange() {
        ReputationHarness h = harness();
        h.handlers.on(registered("evt-r1", AUTHOR, Role.STUDENT));

        h.handlers.on(registered("evt-r2", AUTHOR, Role.TEACHER));

        assertThat(h.accounts.get(AUTHOR).availablePoints()).isZero();
    }

    @Test
    @DisplayName("不同 eventId 的提交/认证事件也只发一次徽章（徽章按存在性去重，而非按事件）")
    void badgeDedupIsByOwnershipNotEventId() {
        ReputationHarness h = harness();

        h.handlers.on(submitted("evt-s1"));
        h.handlers.on(submitted("evt-s2"));
        h.handlers.on(endorsed("evt-e1"));
        h.handlers.on(endorsed("evt-e2"));

        assertThat(h.badges.findByUser(AUTHOR)).extracting(BadgeAward::type)
                .containsExactlyInAnyOrder(BadgeType.FIRST_ANSWER, BadgeType.TEACHER_ENDORSED);
        assertThat(h.events.ofType(BadgeAwardedEvent.class)).hasSize(2);
    }

    @Test
    @DisplayName("整条事件流整体重放一遍：声誉、流水、徽章与首次处理完全一致")
    void fullStreamReplayIsNoop() {
        ReputationHarness h = harness();
        Runnable stream = () -> {
            h.handlers.on(registered("evt-r1", AUTHOR, Role.STUDENT));
            h.handlers.on(submitted("evt-s1"));
            h.handlers.on(upvote("evt-v1"));
            h.handlers.on(endorsed("evt-e1"));
            h.handlers.on(accepted("evt-a1"));
        };
        stream.run();
        int reputation = h.accounts.get(AUTHOR).reputation();
        int ledgerSize = h.ledger.entries().size();
        int badgeEvents = h.events.ofType(BadgeAwardedEvent.class).size();

        stream.run();

        assertThat(reputation).isEqualTo(10 + 15);
        assertThat(h.accounts.get(AUTHOR).reputation()).isEqualTo(reputation);
        assertThat(h.ledger.entries()).hasSize(ledgerSize);
        assertThat(h.events.ofType(BadgeAwardedEvent.class)).hasSize(badgeEvents);
        assertThat(h.badges.findByUser(AUTHOR)).hasSize(3);
    }
}
