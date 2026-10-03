package com.aiuse.medreminder;

import com.aiuse.medreminder.entity.Elder;
import com.aiuse.medreminder.mapper.ElderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口层测试（MockMvc）：验证统一响应体、参数校验与 404 语义。
 * 与 scripts/smoke-test.ps1 的区别：这里是 JUnit 内部测试，不依赖外部启动的服务。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("老人档案接口测试")
class ElderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ElderMapper elderMapper;

    private byte[] json(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("POST /api/elders 返回 code=0，并回填 id")
    void createElder() throws Exception {
        mockMvc.perform(post("/api/elders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"name\":\"ApiTester\",\"age\":80,\"gender\":\"M\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.age").value(80));
    }

    @Test
    @DisplayName("缺少姓名返回 code=400")
    void createElderWithoutName() throws Exception {
        mockMvc.perform(post("/api/elders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"age\":70}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("年龄非法返回 code=400")
    void createElderWithBadAge() throws Exception {
        mockMvc.perform(post("/api/elders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("{\"name\":\"ApiTester\",\"age\":200}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("查询不存在的老人返回 code=404")
    void getUnknownElder() throws Exception {
        mockMvc.perform(get("/api/elders/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("健康检查返回 code=0 且 db=UP")
    void health() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.db").value("UP"));
    }

    @Test
    @DisplayName("删除老人返回 code=0，删除后查询为 404")
    void deleteElder() throws Exception {
        Elder elder = new Elder();
        elder.setName("ApiDeleteTester");
        elder.setAge(72);
        elderMapper.insert(elder);

        mockMvc.perform(delete("/api/elders/" + elder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/elders/" + elder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}

