package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserInfo {
    private Long memberId;
    private String username;
    private String nickname;
    private String phone;
    private String avatar;
    private String role;
    private Long levelId;
    private String levelName;
    private BigDecimal discountRate;
    private BigDecimal totalConsumption;
    private String status;
    private Boolean levelManuallyAdjusted;
    private LocalDateTime createdAt;
}
