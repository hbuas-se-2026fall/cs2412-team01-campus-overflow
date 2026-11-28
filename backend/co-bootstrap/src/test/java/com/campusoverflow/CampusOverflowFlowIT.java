package com.campusoverflow;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusoverflow.identity.application.AdminUserService;
import com.campusoverflow.identity.application.CreateStaffCommand;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 端到端集成测试（质量场景 R-2 的验收）：注册 → 登录 → 提问 → 回答 → 采纳 → 声誉结算 → 搜索命中 → 收到通知。
 * 使用真实的 MySQL 8 与 Redis 7（Testcontainers），因此也验证了 Flyway 迁移、ngram 全文索引与发件箱中继。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
        "co.outbox.poll-interval-ms=200",
        "spring.flyway.clean-disabled=false"
})
class CampusOverflowFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.39"))
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci",
                    "--ngram-token-size=2");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    AdminUserService admins;

    @Test
    void 提问到采纳的完整链路会异步结算声誉并更新搜索与通知() throws Exception {
        register("2026001", "测试小明", "it-ming@campus.edu.cn");
        long hongId = register("2026002", "测试小红", "it-hong@campus.edu.cn");
        MockHttpSession ming = login("2026001");
        MockHttpSession hong = login("2026002");

        long questionId = idOf(mvc.perform(post("/api/v1/questions").session(ming).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"跳表和红黑树在工程上如何取舍？",
                                 "body":"课程项目里需要一个有序结构，二者的实现复杂度和并发友好程度差别很大，应该怎么选？",
                                 "tags":["数据结构","跳表"]}
                                """))
                .andExpect(status().isCreated()).andReturn());

        long answerId = idOf(mvc.perform(post("/api/v1/questions/" + questionId + "/answers").session(hong).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"并发场景优先跳表：它的插入删除只需局部指针操作，容易做成无锁结构；单线程内存场景红黑树更省内存。"}
                                """))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(put("/api/v1/questions/" + questionId + "/accepted-answer").session(ming).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerId\":" + answerId + "}"))
                .andExpect(status().isNoContent());

        // 采纳后：回答者 +15 声誉（经发件箱异步结算）
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/v1/users/" + hongId + "/reputation"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.reputation").value(15))
                        .andExpect(jsonPath("$.badges[?(@.code=='FIRST_ACCEPTED')]").exists()));

        // 搜索投影：中文关键词可命中，且标记为已采纳
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/v1/search/questions").param("q", "跳表"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.items[0].id").value(questionId))
                        .andExpect(jsonPath("$.items[0].accepted").value(true)));

        // 通知中心：回答者收到“被采纳”通知
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                mvc.perform(get("/api/v1/notifications").session(hong))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.items[?(@.type=='ANSWER_ACCEPTED')]").exists()));
    }

    @Test
    void 不能给自己的内容投票() throws Exception {
        register("2026003", "测试小刚", "it-gang@campus.edu.cn");
        MockHttpSession gang = login("2026003");
        long questionId = idOf(mvc.perform(post("/api/v1/questions").session(gang).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"为什么哈希表的负载因子一般取 0.75？",
                                 "body":"JDK HashMap 默认负载因子是 0.75，这个取值是怎么权衡空间与冲突概率得到的？",
                                 "tags":["数据结构"]}
                                """))
                .andExpect(status().isCreated()).andReturn());

        mvc.perform(put("/api/v1/votes").session(gang).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"QUESTION\",\"targetId\":" + questionId + ",\"value\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QA-1015"));
    }

    @Test
    void 未登录不能提问且缺少CSRF令牌会被拒绝() throws Exception {
        mvc.perform(post("/api/v1/questions").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"未登录的提问测试标题\",\"body\":\"这是一段足够长的正文用于通过参数校验。\",\"tags\":[\"测试\"]}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"2026001\",\"password\":\"Campus2026\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 按标签浏览与排序筛选在数据库层生效() throws Exception {
        register("2026010", "测试小林", "it-lin@campus.edu.cn");
        register("2026011", "测试小周", "it-zhou@campus.edu.cn");
        MockHttpSession lin = login("2026010");
        MockHttpSession zhou = login("2026011");

        long votedQuestion = idOf(mvc.perform(post("/api/v1/questions").session(lin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Go 的 GMP 调度器如何避免全局队列竞争？",
                                 "body":"想了解 work stealing 在多核下如何减少全局队列竞争，以及和固定线程池方案的差异。",
                                 "tags":["端到端筛选验证","并发编程"]}
                                """))
                .andExpect(status().isCreated()).andReturn());

        long newerQuestion = idOf(mvc.perform(post("/api/v1/questions").session(lin).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"MySQL 的间隙锁在可重复读下解决了什么问题？",
                                 "body":"记录锁之外还需要间隙锁，主要是为了防止幻读吗？为什么只在可重复读隔离级别下生效？",
                                 "tags":["端到端筛选验证","数据库"]}
                                """))
                .andExpect(status().isCreated()).andReturn());

        // 给较早的问题投一票，使得分排序与时间排序结果相反
        mvc.perform(put("/api/v1/votes").session(zhou).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"QUESTION\",\"targetId\":" + votedQuestion + ",\"value\":1}"))
                .andExpect(status().isOk());

        // 标签浏览：只返回带该标签的问题
        mvc.perform(get("/api/v1/tags/{tag}/questions", "端到端筛选验证"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        // 不存在的标签返回空页而不是 404
        mvc.perform(get("/api/v1/tags/{tag}/questions", "端到端不存在的标签"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // 得分排序：被投票的那条排第一
        mvc.perform(get("/api/v1/questions").param("tag", "端到端筛选验证").param("sort", "votes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(votedQuestion))
                .andExpect(jsonPath("$.items[0].score").value(1));

        // 最新排序：后发布的那条排第一
        mvc.perform(get("/api/v1/questions").param("tag", "端到端筛选验证").param("sort", "newest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(newerQuestion));

        // 未回答筛选：两条都还没人回答
        mvc.perform(get("/api/v1/questions").param("tag", "端到端筛选验证").param("unanswered", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        // 非法排序参数 → 400
        mvc.perform(get("/api/v1/questions").param("sort", "hot"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 管理员禁用账号后该用户无法再登录() throws Exception {
        // 与 DemoDataSeeder 相同：用引导主体创建第一个管理员（0L 为系统主体，不对应任何真实用户）
        // 本类排除了 Spring Session，会话失效由 DisableUserSessionRevocationIT 验证
        long adminId = admins.createStaff(new Actor(0L, "system", Role.ADMIN), new CreateStaffCommand("A9001",
                "测试管理员", "it-admin@campus.edu.cn", "Campus2026", Role.ADMIN, "信息中心"));
        long fangId = register("2026020", "测试小方", "it-fang@campus.edu.cn");
        register("2026021", "测试小陆", "it-lu@campus.edu.cn");
        MockHttpSession admin = login("A9001");
        MockHttpSession lu = login("2026021");

        // 非管理员被 URL 级授权拦截
        mvc.perform(post("/api/v1/admin/users/" + fangId + "/disable").session(lu).with(csrf()))
                .andExpect(status().isForbidden());

        // 管理员不能禁用自己
        mvc.perform(post("/api/v1/admin/users/" + adminId + "/disable").session(admin).with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ID-1007"));

        mvc.perform(post("/api/v1/admin/users/" + fangId + "/disable").session(admin).with(csrf()))
                .andExpect(status().isNoContent());

        // 重复禁用 → 409
        mvc.perform(post("/api/v1/admin/users/" + fangId + "/disable").session(admin).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ID-2007"));

        // 被禁用的账号再次登录失败
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"2026020\",\"password\":\"Campus2026\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 我的资料可以修改学院与班级而身份字段保持不变() throws Exception {
        register("2026030", "测试小舟", "it-profile@campus.edu.cn");
        MockHttpSession zhou = login("2026030");

        // 只改班级：学院保持原值，学号/昵称不被改动
        mvc.perform(put("/api/v1/auth/me").session(zhou).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"className\":\"软工2302\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.className").value("软工2302"))
                .andExpect(jsonPath("$.college").value("计算机工程学院"))
                .andExpect(jsonPath("$.username").value("2026030"))
                .andExpect(jsonPath("$.displayName").value("测试小舟"));

        // 请求体夹带身份字段：被忽略，不能自助改昵称
        mvc.perform(put("/api/v1/auth/me").session(zhou).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"被别人改了\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("测试小舟"));

        // 清空学院：再次 GET 应看到 null，班级保留
        mvc.perform(put("/api/v1/auth/me").session(zhou).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"college\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.college").value(nullValue()));

        mvc.perform(get("/api/v1/auth/me").session(zhou))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.college").value(nullValue()))
                .andExpect(jsonPath("$.className").value("软工2302"));

        // 未登录 → 401
        mvc.perform(put("/api/v1/auth/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"className\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    private long register(String username, String displayName, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterPayload(username, displayName, email,
                                "Campus2026", "计算机工程学院", "计科2301"))))
                .andExpect(status().isCreated()).andReturn();
        return idOf(result);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Campus2026\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long idOf(MvcResult result) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private record RegisterPayload(String username, String displayName, String email, String password,
                                   String college, String className) {
    }
}
