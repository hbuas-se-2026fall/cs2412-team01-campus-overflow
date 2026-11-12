package com.campusoverflow.qa.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusoverflow.qa.application.command.QuestionCommandService;
import com.campusoverflow.qa.application.query.QuestionQueryService;
import com.campusoverflow.qa.application.query.QuestionSummaryView;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 问题接口的 HTTP 契约：分页夹紧、参数校验、状态码。 */
@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private QuestionCommandService commands;

    @Mock
    private QuestionQueryService queries;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(commands, queries))
                .setCustomArgumentResolvers(new TestActorResolver())
                .build();
    }

    @Test
    void listUsesDefaultPaging() throws Exception {
        given(queries.list(isNull(), any(PageRequest.class)))
                .willReturn(PageResult.empty(1, 20));

        mvc.perform(get("/api/v1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        verify(queries).list(null, new PageRequest(1, 20));
    }

    @Test
    void listClampsOutOfRangePaging() throws Exception {
        given(queries.list(any(), any(PageRequest.class))).willReturn(PageResult.empty(1, 50));

        mvc.perform(get("/api/v1/questions").param("page", "0").param("size", "999"))
                .andExpect(status().isOk());

        // 越界参数在接口层被夹紧成 page=1 / size=50，不把脏值透给应用层。
        verify(queries).list(null, new PageRequest(1, 50));
    }

    @Test
    void listPassesCourseFilter() throws Exception {
        given(queries.list(eq(3L), any(PageRequest.class))).willReturn(PageResult.empty(1, 20));

        mvc.perform(get("/api/v1/questions").param("courseId", "3"))
                .andExpect(status().isOk());

        verify(queries).list(3L, new PageRequest(1, 20));
    }

    @Test
    void listReturnsItemsFromQueryService() throws Exception {
        QuestionSummaryView view = new QuestionSummaryView(11L, "跳表怎么实现", "摘要", List.of("数据结构"), 3L,
                null, 5, 2, 40, false, java.time.Instant.parse("2026-11-12T04:00:00Z"));
        given(queries.list(any(), any(PageRequest.class)))
                .willReturn(new PageResult<>(List.of(view), 1, 1, 20));

        mvc.perform(get("/api/v1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(11))
                .andExpect(jsonPath("$.items[0].title").value("跳表怎么实现"))
                .andExpect(jsonPath("$.items[0].tags[0]").value("数据结构"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void postReturns201WithNewId() throws Exception {
        given(commands.post(any(), any())).willReturn(42L);

        mvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":3,"title":"跳表怎么实现","body":"想了解层数如何决定","tags":["数据结构","Java"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42));

        verify(commands).post(any(), any());
    }

    @Test
    void postRejectsBlankTitle() throws Exception {
        mvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":3,"title":"  ","body":"正文","tags":["数据结构"]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commands);
    }

    @Test
    void postRejectsBlankBody() throws Exception {
        mvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":3,"title":"标题","body":"","tags":["数据结构"]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commands);
    }

    @Test
    void postRejectsTooManyTags() throws Exception {
        mvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":3,"title":"标题","body":"正文","tags":["a","b","c","d","e","f"]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commands);
    }

    @Test
    void postRejectsEmptyTagList() throws Exception {
        mvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":3,"title":"标题","body":"正文","tags":[]}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(commands);
    }

    @Test
    void detailPassesCurrentActor() {
        // detail 的 actor 参数是 Optional<Actor>，Spring 的 standalone 解析器会把它当表单对象绑定，
        // 因此这里直接调用控制器方法，只验证「身份原样透传给应用层」这一契约。
        new QuestionController(commands, queries).detail(11L, Optional.of(TestActorResolver.ACTOR));

        verify(queries).detail(11L, TestActorResolver.ACTOR);
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/v1/questions/11"))
                .andExpect(status().isNoContent());

        verify(commands).delete(TestActorResolver.ACTOR, 11L);
    }
}
