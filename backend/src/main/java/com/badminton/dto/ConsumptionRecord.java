package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ConsumptionRecord {
    private String consumeType;
    private String refNo;
    private String description;
    private BigDecimal amount;
    private LocalDateTime consumeAt;
}
