package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtOverviewRow {
    private Long courtId;
    private String courtName;
    private String courtNumber;
    private BigDecimal hourlyRate;
    private String operationalStatus;
    private Long availableSlots;
    private Long bookedSlots;
    private Long inUseSlots;
    private String currentStatus;
}
