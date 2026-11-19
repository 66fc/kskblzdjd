package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AvailabilityRow {
    private Long venueId;
    private String venueName;
    private Long courtId;
    private String courtName;
    private BigDecimal hourlyRate;
    private Long freeSlots;
}
