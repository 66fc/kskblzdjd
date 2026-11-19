package com.badminton.security;

/**
 * 登录态中的当前用户信息。
 */
public record AuthUser(Long memberId, String username, String nickname, String role) {
}
