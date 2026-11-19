package com.badminton.controller;

import com.badminton.common.ApiResponse;
import com.badminton.entity.MemberLevel;
import com.badminton.service.MemberLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "C端-会员等级", description = "公开的会员等级与权益")
public class PublicMemberLevelController {

    private final MemberLevelService levelService;

    public PublicMemberLevelController(MemberLevelService levelService) {
        this.levelService = levelService;
    }

    @GetMapping("/member-levels")
    @Operation(summary = "启用中的会员等级与折扣")
    public ApiResponse<List<MemberLevel>> levels() {
        return ApiResponse.ok(levelService.enabledLevels());
    }
}
