package com.aiuse.medreminder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 账号接口测试：注册、登录、退出、未登录拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("账号接口测试")
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueUsername() {
        return "tester" + System.nanoTime() % 1000000;
    }

    private byte[] json(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private String registerBody(String username) {
        return "{\"username\":\"" + username + "\",\"password\":\"abc123456\"}";
    }

    @Test
    @DisplayName("未登录访问数据接口返回 401")
    void anonymousIsBlocked() throws Exception {
        mockMvc.perform(get("/api/elders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/plans").param("elderId", "1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录状态接口无需登录即可访问")
    void statusIsPublic() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loggedIn").value(false));
    }

    @Test
    @DisplayName("注册成功后自动登录，可访问数据接口")
    void registerThenAccess() throws Exception {
        String username = uniqueUsername();
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(username))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/elders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.data.username").value(username));
    }

    @Test
    @DisplayName("重复用户名注册失败")
    void duplicateUsernameRejected() throws Exception {
        String username = uniqueUsername();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(registerBody(username))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(registerBody(username))))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("密码错误无法登录，退出后接口重新变成 401")
    void loginWrongPasswordAndLogout() throws Exception {
        String username = uniqueUsername();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(json(registerBody(username))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"username\":\"" + username + "\",\"password\":\"wrong-pass\"}")))
                .andExpect(jsonPath("$.code").value(400));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerBody(username))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/api/elders").session(session))
                .andExpect(status().isUnauthorized());
    }
}

