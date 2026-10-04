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

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 访问口令测试：单独起一个"启用了口令"的上下文，验证公网分享时的访问控制。
 * 注意用独立的 properties，避免影响其他测试（默认不启用口令）。
 */
@SpringBootTest(properties = "app.access.password=test-password")
@AutoConfigureMockMvc
@DisplayName("访问口令测试")
class AccessAuthTest {

    private static final String PASSWORD = "test-password";

    @Autowired
    private MockMvc mockMvc;

    private byte[] json(String password) {
        return ("{\"password\":\"" + password + "\"}").getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("未登录访问数据接口返回 401")
    void anonymousIsBlocked() throws Exception {
        mockMvc.perform(get("/api/elders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录状态接口本身无需登录")
    void statusIsPublic() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.required").value(true))
                .andExpect(jsonPath("$.data.loggedIn").value(false));
    }

    @Test
    @DisplayName("口令错误返回 code=400")
    void wrongPasswordRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("wrong-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("口令正确后可正常访问数据接口")
    void loginThenAccessData() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.loggedIn").value(true))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/elders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}

