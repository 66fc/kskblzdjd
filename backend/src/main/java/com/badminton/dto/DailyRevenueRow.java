package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyRevenueRow {
    private LocalDate statDate;
    private BigDecimal amount;
    private Long minutes;
    private Long orderCount;
}
