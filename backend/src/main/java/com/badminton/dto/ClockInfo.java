package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ClockInfo {
    private LocalDateTime effectiveNow;
    private LocalDate businessDate;
    private LocalDateTime simulatedNow;
    private LocalDateTime baseRealAt;
    private LocalDateTime realNow;
    private String clockMode;
    private Boolean paused;
    private BigDecimal speed;
    private Boolean demoMode;
    private LocalDateTime updatedAt;
}
