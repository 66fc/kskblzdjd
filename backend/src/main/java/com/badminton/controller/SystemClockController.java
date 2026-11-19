package com.badminton.controller;

import com.badminton.aspect.AdminLog;
import com.badminton.common.ApiResponse;
import com.badminton.dto.ClockInfo;
import com.badminton.dto.ClockUpdateRequest;
import com.badminton.security.CurrentUser;
import com.badminton.service.SimulatedClockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "模拟时钟", description = "统一系统时间服务，管理员可切换模拟日期/暂停/流速")
public class SystemClockController {

    private final SimulatedClockService clockService;

    public SystemClockController(SimulatedClockService clockService) {
        this.clockService = clockService;
    }

    @GetMapping("/system/clock")
    @Operation(summary = "获取当前模拟时间")
    public ApiResponse<ClockInfo> clock() {
        return ApiResponse.ok(clockService.info());
    }

    @PutMapping("/admin/system/clock")
    @AdminLog(module = "SYSTEM_CLOCK", action = "UPDATE_CLOCK", targetType = "SYSTEM", detail = "修改模拟时钟")
    @Operation(summary = "修改模拟时钟（设置日期/时间、暂停、恢复、流速、模式）")
    public ApiResponse<ClockInfo> update(@Valid @RequestBody ClockUpdateRequest request) {
        return ApiResponse.ok("模拟时钟已更新", clockService.update(request, CurrentUser.id()));
    }
}
