package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.config.LlmProperties;
import com.aiuse.medreminder.dto.AskRequest;
import com.aiuse.medreminder.dto.MedicineAnalysisRequest;
import com.aiuse.medreminder.service.MedicationAssistantService;
import com.aiuse.medreminder.vo.AskAnswer;
import com.aiuse.medreminder.vo.MedicineAnalysis;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用药智能助手：语音/文字问药、药盒说明书解析。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final MedicationAssistantService assistantService;
    private final LlmProperties llmProperties;

    public AiController(MedicationAssistantService assistantService, LlmProperties llmProperties) {
        this.assistantService = assistantService;
        this.llmProperties = llmProperties;
    }

    @PostMapping("/ask")
    public ApiResponse<AskAnswer> ask(@Valid @RequestBody AskRequest request) {
        return ApiResponse.ok(assistantService.ask(request.elderId(), request.question(), request.dialect()));
    }

    @PostMapping("/medicine-analysis")
    public ApiResponse<MedicineAnalysis> analyze(@Valid @RequestBody MedicineAnalysisRequest request) {
        return ApiResponse.ok(assistantService.analyzeMedicine(request.elderId(), request.text()));
    }

    /** 前端用来显示当前是"大模型模式"还是"内置规则模式"。 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("llmEnabled", llmProperties.isUsable());
        data.put("pluginEnabled", llmProperties.isEnabled());
        data.put("model", llmProperties.getModel());
        data.put("mode", llmProperties.isUsable() ? "LLM" : "RULE");
        return ApiResponse.ok(data);
    }
}

