package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CourtDailySummary {
    private LocalDate statDate;
    private Long totalActualMinutes;
    private BigDecimal totalActualHours;
    private BigDecimal totalSalesAmount;
    private Integer usedCourtCount;
    private Integer usageRecordCount;
}
