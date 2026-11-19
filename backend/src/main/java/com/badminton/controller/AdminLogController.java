package com.badminton.controller;

import com.badminton.common.ApiResponse;
import com.badminton.common.PageResult;
import com.badminton.entity.AdminOperationLog;
import com.badminton.mapper.AdminOperationLogMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "后台-操作日志", description = "管理员操作审计日志")
public class AdminLogController {

    private final AdminOperationLogMapper logMapper;

    public AdminLogController(AdminOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @GetMapping
    @Operation(summary = "操作日志列表")
    public ApiResponse<PageResult<AdminOperationLog>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "15") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        IPage<AdminOperationLog> result = logMapper.selectPageFiltered(new Page<>(page, size),
                module, keyword, date);
        return ApiResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(),
                result.getCurrent(), result.getSize()));
    }
}
