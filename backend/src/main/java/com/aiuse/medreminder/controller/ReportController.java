package com.aiuse.medreminder.controller;

import com.aiuse.medreminder.common.ApiResponse;
import com.aiuse.medreminder.service.ReportService;
import com.aiuse.medreminder.vo.AdherenceReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 服药报告：周报 / 月报（子女看漏服次数，医生看依从性）。
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * @param period week 或 month
     * @param date   任意一天，用来定位是哪一周/哪一月，不传默认今天
     */
    @GetMapping("/adherence")
    public ApiResponse<AdherenceReport> adherence(
            @RequestParam long elderId,
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reportService.report(elderId, period, date));
    }
}

