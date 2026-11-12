package com.campusoverflow.qa.interfaces;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusoverflow.qa.application.query.QuestionQueryService;
import com.campusoverflow.qa.application.query.TagView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 标签接口的 HTTP 契约。 */
@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private QuestionQueryService queries;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TagController(queries))
                .setCustomArgumentResolvers(new TestActorResolver())
                .build();
    }

    @Test
    void popularUsesDefaultLimit() throws Exception {
        given(queries.popularTags(20)).willReturn(List.of(new TagView("数据结构", 12L)));

        mvc.perform(get("/api/v1/tags/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("数据结构"))
                .andExpect(jsonPath("$[0].questionCount").value(12));

        verify(queries).popularTags(20);
    }

    @Test
    void popularRespectsCustomLimit() throws Exception {
        given(queries.popularTags(5)).willReturn(List.of());

        mvc.perform(get("/api/v1/tags/popular").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(queries).popularTags(5);
    }
}
