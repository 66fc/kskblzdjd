package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("court")
public class Court {
    @TableId(type = IdType.AUTO)
    private Long courtId;
    private Long venueId;
    private String courtName;
    private String courtNumber;
    private BigDecimal hourlyRate;
    private String operationalStatus;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String venueName;
    @TableField(exist = false)
    private Integer totalBookings;
    @TableField(exist = false)
    private Long todayBookedSlots;
}
