package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.dto.MedicationPlanCreateRequest;
import com.aiuse.medreminder.dto.MedicationPlanUpdateRequest;
import com.aiuse.medreminder.entity.MedicationPlan;
import com.aiuse.medreminder.service.MedicationPlanService;
import com.aiuse.medreminder.vo.TodayDoseSummary;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药计划：加药、改药、停用/恢复、删除、按老人查询、今日服药清单。
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

    /**
     * 今日服药清单（前端打卡页的主接口）。date 不传默认今天。
     */
    @GetMapping("/today")
    public ApiResponse<TodayDoseSummary> today(
            @RequestParam long elderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(planService.today(elderId, date));
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicationPlan> get(@PathVariable long id) {
        return ApiResponse.ok(planService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicationPlan> update(@PathVariable long id,
                                              @Valid @RequestBody MedicationPlanUpdateRequest request) {
        return ApiResponse.ok(planService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<MedicationPlan> updateStatus(@PathVariable long id, @RequestParam int status) {
        return ApiResponse.ok(planService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        planService.delete(id);
        return ApiResponse.ok();
    }
}

