package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtUsageRow {
    private Long courtId;
    private String courtName;
    private String courtNumber;
    private String operationalStatus;
    private BigDecimal hourlyRate;
    private Long minutes;
    private BigDecimal amount;
}
