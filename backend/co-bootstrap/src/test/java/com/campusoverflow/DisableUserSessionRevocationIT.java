package com.campusoverflow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusoverflow.identity.application.AdminUserService;
import com.campusoverflow.identity.application.CreateStaffCommand;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * PB-40「禁用违规账号并使其立即下线」的会话失效验证（ADR-003）。
 * 与 {@link CampusOverflowFlowIT} 不同，本类启用 Spring Session（Redis indexed），
 * 并用真实的 SESSION Cookie 访问，才能守住 {@code AdminUserService.disable} 中的 {@code sessions.revokeAll}。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DisableUserSessionRevocationIT {

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
    void 管理员禁用账号后该用户已登录的会话立即失效() throws Exception {
        // 0L 为系统主体（与 DemoDataSeeder 一致），用于创建第一个管理员
        admins.createStaff(new Actor(0L, "system", Role.ADMIN), new CreateStaffCommand("A9101",
                "测试管理员", "it-admin2@campus.edu.cn", "Campus2026", Role.ADMIN, "信息中心"));
        long fangId = register("2026120", "测试小方二", "it-fang2@campus.edu.cn");
        Cookie admin = login("A9101");
        Cookie fang = login("2026120");

        // 禁用前会话有效
        mvc.perform(get("/api/v1/auth/me").cookie(fang))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/admin/users/" + fangId + "/disable").cookie(admin).with(csrf()))
                .andExpect(status().isNoContent());

        // 关键断言：已登录的会话被服务端踢掉
        mvc.perform(get("/api/v1/auth/me").cookie(fang))
                .andExpect(status().isUnauthorized());

        // 管理员自己的会话不受影响
        mvc.perform(get("/api/v1/auth/me").cookie(admin))
                .andExpect(status().isOk());
    }

    private long register(String username, String displayName, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterPayload(username, displayName, email,
                                "Campus2026", "计算机工程学院", "计科2301"))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Cookie login(String username) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Campus2026\"}"))
                .andExpect(status().isOk()).andReturn();
        return Objects.requireNonNull(result.getResponse().getCookie("SESSION"), "登录响应未下发 SESSION Cookie");
    }

    private record RegisterPayload(String username, String displayName, String email, String password,
                                   String college, String className) {
    }
}
