package com.badminton.controller;

import com.badminton.aspect.AdminLog;
import com.badminton.common.ApiResponse;
import com.badminton.common.PageResult;
import com.badminton.dto.CourtRequest;
import com.badminton.dto.SlotGenerateRequest;
import com.badminton.entity.Court;
import com.badminton.entity.CourtSlot;
import com.badminton.entity.CourtUsageRecord;
import com.badminton.service.CourtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Map;

@RestController
@RequestMapping("/api/admin/courts")
@Tag(name = "后台-场地管理", description = "场地的增删改查、时间段维护、历史使用记录")
public class AdminCourtController {

    private final CourtService courtService;

    public AdminCourtController(CourtService courtService) {
        this.courtService = courtService;
    }

    @GetMapping
    @Operation(summary = "场地列表（含预约统计）")
    public ApiResponse<List<Court>> list(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) Long venueId) {
        return ApiResponse.ok(courtService.list(status, venueId));
    }

    @PostMapping
    @AdminLog(module = "COURT", action = "CREATE_COURT", targetType = "COURT", detail = "新增场地")
    @Operation(summary = "新增场地")
    public ApiResponse<Court> create(@Valid @RequestBody CourtRequest request) {
        return ApiResponse.ok("场地创建成功", courtService.create(request));
    }

    @PutMapping("/{courtId}")
    @AdminLog(module = "COURT", action = "UPDATE_COURT", targetType = "COURT", detail = "修改场地")
    @Operation(summary = "修改场地")
    public ApiResponse<Court> update(@PathVariable Long courtId, @Valid @RequestBody CourtRequest request) {
        return ApiResponse.ok("场地更新成功", courtService.update(courtId, request));
    }

    @DeleteMapping("/{courtId}")
    @AdminLog(module = "COURT", action = "DELETE_COURT", targetType = "COURT", detail = "删除场地")
    @Operation(summary = "删除场地（有历史记录时自动转为停用）")
    public ApiResponse<String> delete(@PathVariable Long courtId) {
        return ApiResponse.ok(courtService.delete(courtId));
    }

    @GetMapping("/{courtId}/slots")
    @Operation(summary = "查看场地某日时间段状态")
    public ApiResponse<List<CourtSlot>> slots(@PathVariable Long courtId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(courtService.slots(courtId, date));
    }

    @PostMapping("/slots/generate")
    @AdminLog(module = "COURT", action = "GENERATE_SLOTS", targetType = "COURT", detail = "生成场地时间段")
    @Operation(summary = "生成未来时间段（调用存储过程 sp_generate_court_slots）")
    public ApiResponse<Map<String, Object>> generate(@Valid @RequestBody SlotGenerateRequest request) {
        return ApiResponse.ok("时间段生成完成", courtService.generateSlots(request.getFrom(), request.getTo()));
    }

    @GetMapping("/{courtId}/usage")
    @Operation(summary = "场地历史使用记录")
    public ApiResponse<PageResult<CourtUsageRecord>> usage(@PathVariable Long courtId,
                                                           @RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size,
                                                           @RequestParam(required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam(required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(courtService.usageHistory(courtId, page, size, from, to));
    }
}
