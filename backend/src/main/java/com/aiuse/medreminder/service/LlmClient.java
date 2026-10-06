package com.aiuse.medreminder.service;

import com.aiuse.medreminder.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * OpenAI 兼容协议的大模型客户端（只用到 chat/completions 一个接口）。
 */
@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final LlmProperties properties;
    private final RestClient restClient = RestClient.create();

    public LlmClient(LlmProperties properties) {
        this.properties = properties;
    }

    public boolean isAvailable() {
        return properties.isUsable();
    }

    /**
     * 调用大模型；失败时返回 null，由调用方降级到规则引擎。
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return null;
        }
        try {
            ChatRequest body = new ChatRequest(
                    properties.getModel(),
                    List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
                    0.3,
                    600);
            ChatResponse response = restClient.post()
                    .uri(properties.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return null;
            }
            Message message = response.choices().get(0).message();
            return message == null ? null : message.content();
        } catch (Exception e) {
            log.warn("调用大模型失败，降级为规则回答: {}", e.getMessage());
            return null;
        }
    }

    public record Message(String role, String content) {
    }

    public record ChatRequest(String model, List<Message> messages, double temperature,
                              Integer max_tokens) {
    }

    public record Choice(Message message) {
    }

    public record ChatResponse(List<Choice> choices) {
    }
}

