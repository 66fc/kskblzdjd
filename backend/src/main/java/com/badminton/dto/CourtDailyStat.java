package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CourtDailyStat {
    private LocalDate statDate;
    private Long courtId;
    private String courtName;
    private Long totalActualMinutes;
    private BigDecimal totalActualHours;
    private BigDecimal totalSalesAmount;
    private Integer usageRecordCount;
}
