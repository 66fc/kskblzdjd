package com.badminton.controller;

import com.badminton.aspect.AdminLog;
import com.badminton.common.ApiResponse;
import com.badminton.common.PageResult;
import com.badminton.dto.LevelChangeRequest;
import com.badminton.dto.MemberUpdateRequest;
import com.badminton.entity.Booking;
import com.badminton.entity.Member;
import com.badminton.entity.MemberLevelChangeLog;
import com.badminton.security.CurrentUser;
import com.badminton.service.BookingService;
import com.badminton.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/members")
@Tag(name = "后台-用户与会员等级", description = "用户资料、禁用/恢复、手动调级与等级变更历史")
public class AdminMemberController {

    private final MemberService memberService;
    private final BookingService bookingService;

    public AdminMemberController(MemberService memberService, BookingService bookingService) {
        this.memberService = memberService;
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(summary = "会员列表")
    public ApiResponse<PageResult<Member>> list(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long levelId,
                                                @RequestParam(required = false) String status) {
        return ApiResponse.ok(memberService.page(page, size, keyword, levelId, status));
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "会员详情（含预约数、消费历史、调级历史）")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long memberId) {
        return ApiResponse.ok(memberService.detail(memberId));
    }

    @PutMapping("/{memberId}")
    @AdminLog(module = "MEMBER", action = "UPDATE_MEMBER", targetType = "MEMBER", detail = "修改会员资料")
    @Operation(summary = "修改会员资料 / 禁用 / 恢复")
    public ApiResponse<Member> update(@PathVariable Long memberId,
                                      @Valid @RequestBody MemberUpdateRequest request) {
        return ApiResponse.ok("会员资料已更新", memberService.update(memberId, request));
    }

    @PutMapping("/{memberId}/level")
    @AdminLog(module = "MEMBER_LEVEL", action = "UPDATE_LEVEL", targetType = "MEMBER", detail = "手动调整会员等级")
    @Operation(summary = "管理员手动调整会员等级（写入变更日志）")
    public ApiResponse<Member> changeLevel(@PathVariable Long memberId,
                                           @Valid @RequestBody LevelChangeRequest request) {
        return ApiResponse.ok("会员等级已更新", memberService.changeLevel(memberId, request, CurrentUser.id()));
    }

    @PostMapping("/{memberId}/reset-auto-level")
    @AdminLog(module = "MEMBER_LEVEL", action = "RESET_AUTO_LEVEL", targetType = "MEMBER", detail = "恢复自动升级")
    @Operation(summary = "恢复自动升级规则")
    public ApiResponse<Member> resetAuto(@PathVariable Long memberId) {
        return ApiResponse.ok("已恢复自动升级", memberService.resetManualLevel(memberId, CurrentUser.id()));
    }

    @GetMapping("/{memberId}/level-history")
    @Operation(summary = "会员等级变更历史")
    public ApiResponse<List<MemberLevelChangeLog>> levelHistory(@PathVariable Long memberId) {
        return ApiResponse.ok(memberService.levelHistory(memberId));
    }

    @GetMapping("/{memberId}/bookings")
    @Operation(summary = "会员预约历史")
    public ApiResponse<PageResult<Booking>> bookings(@PathVariable Long memberId,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String status) {
        return ApiResponse.ok(bookingService.mine(page, size, memberId, status));
    }
}
