package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.service.MedicationPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用药计划：加药、按老人查询。
 */
@RestController
@RequestMapping("/api/plans")
public class MedicationPlanController {

    private final MedicationPlanService planService;

    public MedicationPlanController(MedicationPlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ApiResponse<MedicationPlan> create(@Valid @RequestBody MedicationPlanCreateRequest request) {
        return ApiResponse.ok(planService.create(request));
    }

    @GetMapping
    public ApiResponse<List<MedicationPlan>> listByElder(@RequestParam long elderId) {
        return ApiResponse.ok(planService.listByElder(elderId));
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicationPlan> get(@PathVariable long id) {
        return ApiResponse.ok(planService.get(id));
    }
}

