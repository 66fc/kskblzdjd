package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("booking")
public class Booking {
    @TableId(type = IdType.AUTO)
    private Long bookingId;
    private String bookingNo;
    private String requestId;
    private Long memberId;
    private Long courtId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer durationMinutes;
    private BigDecimal hourlyRateSnapshot;
    private BigDecimal discountSnapshot;
    private BigDecimal originalAmount;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private String status;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;

    @TableField(exist = false)
    private String memberNickname;
    @TableField(exist = false)
    private String memberPhone;
    @TableField(exist = false)
    private String memberLevelName;
    @TableField(exist = false)
    private String courtName;
    @TableField(exist = false)
    private String courtNumber;
    @TableField(exist = false)
    private String venueName;
}
