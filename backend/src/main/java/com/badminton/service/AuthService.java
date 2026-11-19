package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.dto.LoginRequest;
import com.badminton.dto.LoginResponse;
import com.badminton.dto.RegisterRequest;
import com.badminton.dto.UserInfo;
import com.badminton.entity.Member;
import com.badminton.entity.MemberLevel;
import com.badminton.mapper.MemberLevelMapper;
import com.badminton.mapper.MemberMapper;
import com.badminton.security.JwtService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final MemberMapper memberMapper;
    private final MemberLevelMapper levelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(MemberMapper memberMapper, MemberLevelMapper levelMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.memberMapper = memberMapper;
        this.levelMapper = levelMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUsername, request.getUsername().trim()));
        if (member == null || !passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new BizException(HttpStatus.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        MemberLevel level = levelMapper.selectById(member.getLevelId());
        String token = jwtService.generate(member.getMemberId(), member.getUsername(), member.getRole());
        return new LoginResponse(token, toUserInfo(member, level));
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        long sameName = memberMapper.selectCount(new LambdaQueryWrapper<Member>()
                .eq(Member::getUsername, request.getUsername().trim()));
        if (sameName > 0) {
            throw BizException.conflict("用户名已存在");
        }
        long samePhone = memberMapper.selectCount(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, request.getPhone()));
        if (samePhone > 0) {
            throw BizException.conflict("手机号已被注册");
        }

        MemberLevel defaultLevel = levelMapper.selectOne(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, "ENABLED")
                .orderByAsc(MemberLevel::getLevelRank)
                .last("LIMIT 1"));
        if (defaultLevel == null) {
            throw new BizException("系统尚未配置可用的会员等级");
        }

        Member member = new Member();
        member.setUsername(request.getUsername().trim());
        member.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        member.setNickname(request.getNickname().trim());
        member.setPhone(request.getPhone());
        member.setRole("MEMBER");
        member.setLevelId(defaultLevel.getLevelId());
        member.setTotalConsumption(java.math.BigDecimal.ZERO);
        member.setStatus("ACTIVE");
        member.setLevelManuallyAdjusted(false);
        memberMapper.insert(member);

        String token = jwtService.generate(member.getMemberId(), member.getUsername(), member.getRole());
        return new LoginResponse(token, toUserInfo(member, defaultLevel));
    }

    public UserInfo me(Long memberId) {
        Member member = memberMapper.selectWithLevel(memberId);
        if (member == null) {
            throw BizException.notFound("用户不存在");
        }
        return toUserInfo(member, null);
    }

    private UserInfo toUserInfo(Member member, MemberLevel level) {
        UserInfo info = new UserInfo();
        info.setMemberId(member.getMemberId());
        info.setUsername(member.getUsername());
        info.setNickname(member.getNickname());
        info.setPhone(member.getPhone());
        info.setAvatar(member.getAvatar());
        info.setRole(member.getRole());
        info.setLevelId(member.getLevelId());
        info.setTotalConsumption(member.getTotalConsumption());
        info.setStatus(member.getStatus());
        info.setLevelManuallyAdjusted(member.getLevelManuallyAdjusted());
        info.setCreatedAt(member.getCreatedAt());
        if (level != null) {
            info.setLevelName(level.getLevelName());
            info.setDiscountRate(level.getDiscountRate());
        } else {
            MemberLevel dbLevel = levelMapper.selectById(member.getLevelId());
            if (dbLevel != null) {
                info.setLevelName(dbLevel.getLevelName());
                info.setDiscountRate(dbLevel.getDiscountRate());
            }
        }
        return info;
    }
}
