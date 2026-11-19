package com.badminton.aspect;

import com.badminton.entity.AdminOperationLog;
import com.badminton.mapper.AdminOperationLogMapper;
import com.badminton.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class AdminLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AdminLogAspect.class);

    private final AdminOperationLogMapper logMapper;

    public AdminLogAspect(AdminOperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Around("@annotation(adminLog)")
    public Object record(ProceedingJoinPoint joinPoint, AdminLog adminLog) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            AuthUser operator = authentication != null && authentication.getPrincipal() instanceof AuthUser user
                    ? user : null;

            AdminOperationLog entry = new AdminOperationLog();
            entry.setOperatorId(operator == null ? null : operator.memberId());
            entry.setOperatorName(operator == null ? "system" : operator.nickname());
            entry.setModule(adminLog.module());
            entry.setAction(adminLog.action());
            entry.setTargetType(adminLog.targetType().isBlank() ? null : adminLog.targetType());
            entry.setTargetId(extractTargetId(joinPoint.getArgs()));
            entry.setDetail(buildDetail(adminLog.detail(), joinPoint.getArgs()));
            entry.setIp(clientIp());
            entry.setCreatedAt(LocalDateTime.now());
            logMapper.insert(entry);
        } catch (Exception ex) {
            log.warn("failed to write admin operation log: {}", ex.getMessage());
        }
        return result;
    }

    private String extractTargetId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long || arg instanceof Integer) {
                return String.valueOf(arg);
            }
        }
        return null;
    }

    private String buildDetail(String prefix, Object[] args) {
        String text = prefix;
        if (args != null && args.length > 0) {
            String joined = Arrays.stream(args)
                    .filter(a -> a != null && !(a instanceof HttpServletRequest))
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            if (!joined.isBlank()) {
                text = (text.isBlank() ? "" : text + " | ") + joined;
            }
        }
        return text.length() > 900 ? text.substring(0, 900) : text;
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }
}
