package com.badminton.controller;

import com.badminton.aspect.AdminLog;
import com.badminton.common.ApiResponse;
import com.badminton.entity.MemberLevel;
import com.badminton.entity.MemberLevelChangeLog;
import com.badminton.service.MemberLevelService;
import com.badminton.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "后台-会员等级配置", description = "会员等级维护与全局调级记录")
public class AdminLevelController {

    private final MemberLevelService levelService;
    private final MemberService memberService;

    public AdminLevelController(MemberLevelService levelService, MemberService memberService) {
        this.levelService = levelService;
        this.memberService = memberService;
    }

    @GetMapping("/member-levels")
    @Operation(summary = "会员等级列表（含人数统计）")
    public ApiResponse<List<MemberLevel>> levels() {
        return ApiResponse.ok(levelService.list());
    }

    @PostMapping("/member-levels")
    @AdminLog(module = "MEMBER_LEVEL", action = "CREATE_LEVEL", targetType = "MEMBER_LEVEL", detail = "新增会员等级")
    @Operation(summary = "新增会员等级")
    public ApiResponse<MemberLevel> create(@Valid @RequestBody MemberLevel level) {
        return ApiResponse.ok("等级创建成功", levelService.create(level));
    }

    @PutMapping("/member-levels/{levelId}")
    @AdminLog(module = "MEMBER_LEVEL", action = "UPDATE_LEVEL_CONFIG", targetType = "MEMBER_LEVEL", detail = "修改会员等级")
    @Operation(summary = "修改会员等级")
    public ApiResponse<MemberLevel> update(@PathVariable Long levelId, @Valid @RequestBody MemberLevel level) {
        return ApiResponse.ok("等级更新成功", levelService.update(levelId, level));
    }

    @DeleteMapping("/member-levels/{levelId}")
    @AdminLog(module = "MEMBER_LEVEL", action = "DELETE_LEVEL", targetType = "MEMBER_LEVEL", detail = "删除会员等级")
    @Operation(summary = "删除会员等级（被使用时转为停用）")
    public ApiResponse<String> delete(@PathVariable Long levelId) {
        return ApiResponse.ok(levelService.delete(levelId));
    }

    @GetMapping("/level-changes")
    @Operation(summary = "最近会员等级变更记录")
    public ApiResponse<List<MemberLevelChangeLog>> recentChanges(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(memberService.recentLevelChanges(limit));
    }
}
