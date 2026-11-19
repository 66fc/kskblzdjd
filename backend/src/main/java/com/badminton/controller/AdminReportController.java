package com.badminton.controller;

import com.badminton.common.ApiResponse;
import com.badminton.dto.CourtDailyStat;
import com.badminton.dto.DailyReportResponse;
import com.badminton.dto.DashboardResponse;
import com.badminton.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "后台-报表统计", description = "数据总览、日使用统计（存储过程）、周/月汇总")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "数据总览")
    public ApiResponse<DashboardResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reportService.dashboard(date));
    }

    @GetMapping("/daily/court")
    @Operation(summary = "日使用统计（全部场地 + 汇总，调用存储过程）")
    public ApiResponse<DailyReportResponse> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reportService.daily(date));
    }

    @GetMapping("/daily/court/{courtId}")
    @Operation(summary = "单一场地日统计（调用存储过程）")
    public ApiResponse<CourtDailyStat> dailyCourt(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reportService.dailyCourt(date, courtId));
    }

    @GetMapping("/overview")
    @Operation(summary = "周/月经营汇总")
    public ApiResponse<Map<String, Object>> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "week") String range) {
        return ApiResponse.ok(reportService.overview(date, range));
    }
}
