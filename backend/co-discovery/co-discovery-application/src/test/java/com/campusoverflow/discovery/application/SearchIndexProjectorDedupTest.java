package com.campusoverflow.discovery.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.InMemoryProcessed;
import com.campusoverflow.discovery.application.fake.DiscoveryTestFixtures.RecordingSearchIndex;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 搜索投影器的去重语义：{@link SearchIndexProjector} 的每个处理器都以
 * {@code if (!firstTime(e)) return;} 开头，因为 {@code adjustAnswerCount} / {@code adjustScore}
 * 这类计数器更新**本身不幂等**，重复执行会把数字算错。
 * <p>
 * {@link SearchIndexProjectorTest} 只对「回答提交」验证过一次去重，其余七个处理器
 * 的去重分支一直没被走到。这里逐个补上：同一个 eventId 投两遍，第二遍必须完全无副作用。
 */
class SearchIndexProjectorDedupTest {

    private static final Instant T = DiscoveryTestFixtures.at("2026-10-27T10:00:00Z");

    private RecordingSearchIndex index;
    private InMemoryProcessed processed;
    private SearchIndexProjector projector;

    @BeforeEach
    void setUp() {
        index = new RecordingSearchIndex();
        processed = new InMemoryProcessed();
        projector = new SearchIndexProjector(index, processed);
    }

    @Test
    @DisplayName("问题发布重放：只插入一条文档，不会出现重复索引项")
    void questionPostedReplayInsertsOnce() {
        QuestionPostedEvent e = new QuestionPostedEvent("d1", T, 11L, 7L, 3L, "红黑树怎么旋转", "正文",
                List.of("java", "算法"));

        projector.on(e);
        projector.on(e);

        assertThat(index.inserts).hasSize(1);
        assertThat(processed.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("问题编辑重放：文本只更新一次")
    void questionEditedReplayUpdatesOnce() {
        QuestionEditedEvent e = new QuestionEditedEvent("d2", T, 11L, "新标题", "新正文", List.of("树"));

        projector.on(e);
        projector.on(e);

        assertThat(index.textUpdates).containsExactly("11|新标题|新正文|树");
    }

    @Test
    @DisplayName("回答提交重放：计数不会被加两次（这是去重最要紧的一处）")
    void answerSubmittedReplayDoesNotDoubleCount() {
        AnswerSubmittedEvent e = new AnswerSubmittedEvent("d3", T, 5L, 11L, 9L, 7L, "红黑树怎么旋转");

        projector.on(e);
        projector.on(e);
        projector.on(e);

        assertThat(index.answerDeltas).containsExactly("11:1");
    }

    @Test
    @DisplayName("删问题重放：软删只执行一次")
    void questionDeletedReplayMarksDeletedOnce() {
        ContentDeletedEvent e = new ContentDeletedEvent("d4", T, "QUESTION", 11L, 11L);

        projector.on(e);
        projector.on(e);

        assertThat(index.deleted).containsExactly(11L);
    }

    @Test
    @DisplayName("删回答重放：不会把回答数减成负数")
    void answerDeletedReplayDecrementsOnce() {
        ContentDeletedEvent e = new ContentDeletedEvent("d5", T, "ANSWER", 5L, 11L);

        projector.on(e);
        projector.on(e);

        assertThat(index.answerDeltas).containsExactly("11:-1");
    }

    @Test
    @DisplayName("采纳重放：accepted 标记只打一次")
    void acceptedReplayMarksOnce() {
        AnswerAcceptedEvent e = new AnswerAcceptedEvent("d6", T, 5L, 11L, 9L, 7L, 7L, "红黑树怎么旋转");

        projector.on(e);
        projector.on(e);

        assertThat(index.accepted).containsExactly(11L);
    }

    @Test
    @DisplayName("投票重放：得分不会被反复加减（撤销再撤销会把分数算飞）")
    void voteReplayAppliesDeltaOnce() {
        VoteCastEvent e = new VoteCastEvent("d7", T, "QUESTION", 11L, 7L, 3L, 11L, 1, -1);

        projector.on(e);
        projector.on(e);

        assertThat(index.scoreDeltas).containsExactly("11:-2");
    }

    @Test
    @DisplayName("悬赏开启重放：旗标只置一次")
    void bountyOpenedReplaySetsFlagOnce() {
        BountyOpenedEvent e = new BountyOpenedEvent("d8", T, 1L, 11L, 7L, 50, T.plus(Duration.ofDays(7)));

        projector.on(e);
        projector.on(e);

        assertThat(index.bountyFlags).containsExactly("11:true");
    }

    @Test
    @DisplayName("悬赏获奖重放：旗标只落一次")
    void bountyAwardedReplaySetsFlagOnce() {
        BountyAwardedEvent e = new BountyAwardedEvent("d9", T, 1L, 11L, 7L, 9L, 50);

        projector.on(e);
        projector.on(e);

        assertThat(index.bountyFlags).containsExactly("11:false");
    }

    @Test
    @DisplayName("悬赏关闭重放：旗标只落一次")
    void bountyClosedReplaySetsFlagOnce() {
        BountyClosedEvent e = new BountyClosedEvent("d10", T, 1L, 11L, 7L, 50);

        projector.on(e);
        projector.on(e);

        assertThat(index.bountyFlags).containsExactly("11:false");
    }

    @Test
    @DisplayName("不同事件但同一问题：各自都生效，去重只按 eventId 而非 questionId")
    void differentEventIdsAreBothApplied() {
        // 同一问题的两次回答提交，eventId 不同 → 两次都该计数
        projector.on(new AnswerSubmittedEvent("d11", T, 5L, 11L, 9L, 7L, "红黑树怎么旋转"));
        projector.on(new AnswerSubmittedEvent("d12", T, 6L, 11L, 8L, 7L, "红黑树怎么旋转"));

        assertThat(index.answerDeltas).containsExactly("11:1", "11:1");
        assertThat(processed.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("去重按消费者隔离：同一 eventId 在别的消费者名下仍应被处理")
    void dedupIsScopedByConsumer() {
        AnswerSubmittedEvent e = new AnswerSubmittedEvent("d13", T, 5L, 11L, 9L, 7L, "红黑树怎么旋转");

        // 先把该 eventId 记到另一个消费者名下
        processed.markProcessed("d13", "discovery.other-consumer");

        // 搜索索引消费者仍应正常处理（两边的 key 不冲突）
        projector.on(e);

        assertThat(index.answerDeltas).containsExactly("11:1");
        assertThat(processed.size()).isEqualTo(2);
    }
}
