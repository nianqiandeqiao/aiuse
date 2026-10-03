package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.dto.CheckinRequest;
import com.aiuse.medreminder.entity.MedicationLog;
import com.aiuse.medreminder.service.CheckinService;
import com.aiuse.medreminder.vo.CheckinStats;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 服药打卡：打卡、撤销、查询、按时率统计。
 */
@RestController
@RequestMapping("/api/checkins")
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping
    public ApiResponse<MedicationLog> checkin(@Valid @RequestBody CheckinRequest request) {
        return ApiResponse.ok(checkinService.checkin(request));
    }

    @GetMapping
    public ApiResponse<List<MedicationLog>> list(
            @RequestParam long elderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(checkinService.listByElder(elderId, date, from, to));
    }

    /**
     * 按时率统计。from/to 都不传时默认统计最近 7 天。
     */
    @GetMapping("/stats")
    public ApiResponse<CheckinStats> stats(
            @RequestParam long elderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(checkinService.stats(elderId, from, to));
    }

    /** 撤销误打卡。 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> undo(@PathVariable long id) {
        checkinService.undo(id);
        return ApiResponse.ok();
    }
}

