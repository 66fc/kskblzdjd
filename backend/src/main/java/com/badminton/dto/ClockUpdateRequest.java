package com.badminton.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClockUpdateRequest {
    /**
     * 目标模拟时间，格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-ddTHH:mm。
     */
    private String simulatedNow;

    /**
     * REAL / SIMULATED
     */
    private String clockMode;

    private Boolean paused;

    @DecimalMin(value = "0.10", message = "时间流速不能小于0.1")
    @DecimalMax(value = "100.00", message = "时间流速不能大于100")
    private BigDecimal speed;

    private Boolean demoMode;

    private String reason;
}
