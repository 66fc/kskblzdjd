package com.badminton.controller;

import com.badminton.common.ApiResponse;
import com.badminton.dto.LoginRequest;
import com.badminton.dto.LoginResponse;
import com.badminton.dto.RegisterRequest;
import com.badminton.dto.UserInfo;
import com.badminton.security.CurrentUser;
import com.badminton.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "认证与用户", description = "注册、登录、当前用户信息")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @Operation(summary = "会员注册")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("注册成功", authService.register(request));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "登录（管理员与会员共用）")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", authService.login(request));
    }

    @GetMapping("/users/me")
    @Operation(summary = "获取当前登录用户信息")
    public ApiResponse<UserInfo> me() {
        return ApiResponse.ok(authService.me(CurrentUser.id()));
    }
}
