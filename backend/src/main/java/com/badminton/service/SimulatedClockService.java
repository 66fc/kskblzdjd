package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.common.DateTimeUtils;
import com.badminton.dto.ClockInfo;
import com.badminton.dto.ClockUpdateRequest;
import com.badminton.entity.SystemSetting;
import com.badminton.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统一模拟时钟服务。
 * 所有业务"当前时间"必须从这里获取，禁止在 Controller 中写死日期，
 * 也禁止直接用数据库 NOW() 作为业务时间（存储过程通过参数接收日期）。
 */
@Service
public class SimulatedClockService {

    private final SystemSettingMapper systemSettingMapper;

    public SimulatedClockService(SystemSettingMapper systemSettingMapper) {
        this.systemSettingMapper = systemSettingMapper;
    }

    public SystemSetting current() {
        SystemSetting setting = systemSettingMapper.selectById(1);
        if (setting == null) {
            setting = new SystemSetting();
            setting.setSettingId(1);
            setting.setSimulatedNow(LocalDateTime.now().withNano(0));
            setting.setBaseRealAt(LocalDateTime.now().withNano(0));
            setting.setClockMode("SIMULATED");
            setting.setClockPaused(false);
            setting.setClockSpeed(BigDecimal.ONE);
            setting.setDemoMode(true);
            systemSettingMapper.insert(setting);
        }
        return setting;
    }

    /**
     * 当前业务时间：
     * REAL      -> 服务器真实时间
     * SIMULATED -> 基准模拟时间 + (真实经过秒数 * 流速)，暂停时冻结。
     */
    public LocalDateTime now() {
        SystemSetting setting = current();
        if ("REAL".equals(setting.getClockMode())) {
            return LocalDateTime.now().withNano(0);
        }
        if (Boolean.TRUE.equals(setting.getClockPaused())) {
            return setting.getSimulatedNow();
        }
        long elapsedSeconds = Duration.between(setting.getBaseRealAt(), LocalDateTime.now()).getSeconds();
        if (elapsedSeconds < 0) {
            elapsedSeconds = 0;
        }
        double speed = setting.getClockSpeed() == null ? 1.0 : setting.getClockSpeed().doubleValue();
        long advancedSeconds = (long) (elapsedSeconds * speed);
        return setting.getSimulatedNow().plusSeconds(advancedSeconds).withNano(0);
    }

    public LocalDate today() {
        return now().toLocalDate();
    }

    public ClockInfo info() {
        SystemSetting setting = current();
        ClockInfo info = new ClockInfo();
        info.setEffectiveNow(now());
        info.setBusinessDate(now().toLocalDate());
        info.setSimulatedNow(setting.getSimulatedNow());
        info.setBaseRealAt(setting.getBaseRealAt());
        info.setRealNow(LocalDateTime.now().withNano(0));
        info.setClockMode(setting.getClockMode());
        info.setPaused(setting.getClockPaused());
        info.setSpeed(setting.getClockSpeed());
        info.setDemoMode(setting.getDemoMode());
        info.setUpdatedAt(setting.getUpdatedAt());
        return info;
    }

    @Transactional
    public ClockInfo update(ClockUpdateRequest request, Long adminId) {
        SystemSetting setting = current();
        LocalDateTime realNow = LocalDateTime.now().withNano(0);
        LocalDateTime effectiveNow = now();

        LocalDateTime targetBase = effectiveNow;
        if (request.getSimulatedNow() != null && !request.getSimulatedNow().isBlank()) {
            targetBase = DateTimeUtils.parseDateTime(request.getSimulatedNow()).withNano(0);
            if (targetBase.getYear() < 2000 || targetBase.getYear() > 2100) {
                throw new BizException("模拟时间需在 2000-2100 年之间");
            }
        }

        String mode = request.getClockMode() != null ? request.getClockMode().toUpperCase() : setting.getClockMode();
        if (!"REAL".equals(mode) && !"SIMULATED".equals(mode)) {
            throw new BizException("时钟模式只能是 REAL 或 SIMULATED");
        }
        if (request.getSimulatedNow() != null && !request.getSimulatedNow().isBlank()
                && "REAL".equals(mode)) {
            // 指定了模拟时间，自动切换到模拟模式，符合演示预期
            mode = "SIMULATED";
        }

        BigDecimal speed = request.getSpeed() != null ? request.getSpeed()
                : (setting.getClockSpeed() == null ? BigDecimal.ONE : setting.getClockSpeed());
        if (speed.compareTo(new BigDecimal("0.10")) < 0 || speed.compareTo(new BigDecimal("100.00")) > 0) {
            throw new BizException("时间流速需在 0.10 - 100.00 之间");
        }

        boolean paused = request.getPaused() != null ? request.getPaused()
                : Boolean.TRUE.equals(setting.getClockPaused());
        boolean demoMode = request.getDemoMode() != null ? request.getDemoMode()
                : Boolean.TRUE.equals(setting.getDemoMode());

        setting.setSimulatedNow(targetBase);
        setting.setBaseRealAt(realNow);
        setting.setClockMode(mode);
        setting.setClockPaused(paused);
        setting.setClockSpeed(speed);
        setting.setDemoMode(demoMode);
        setting.setUpdatedBy(adminId);
        systemSettingMapper.updateById(setting);

        return info();
    }
}
