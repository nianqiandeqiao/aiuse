package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.dto.DrugNamesRequest;
import com.aiuse.medreminder.dto.InteractionCheckRequest;
import com.aiuse.medreminder.entity.InteractionRule;
import com.aiuse.medreminder.service.InteractionService;
import com.aiuse.medreminder.vo.InteractionCheckResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用药安全：相互作用检查、重复用药检查、规则库查询。
 */
@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    /** 按老人检查：自动读取该老人所有进行中的用药计划。 */
    @PostMapping("/check")
    public ApiResponse<InteractionCheckResult> check(@Valid @RequestBody InteractionCheckRequest request) {
        return ApiResponse.ok(interactionService.checkByElder(request.elderId()));
    }

    /** 按药名检查：医生开药前预检、前端加药时即时提示。 */
    @PostMapping("/check-drugs")
    public ApiResponse<InteractionCheckResult> checkDrugs(@Valid @RequestBody DrugNamesRequest request) {
        return ApiResponse.ok(interactionService.checkDrugNames(request.drugNames()));
    }

    /** 规则库列表。 */
    @GetMapping("/rules")
    public ApiResponse<List<InteractionRule>> rules() {
        return ApiResponse.ok(interactionService.listRules());
    }
}

