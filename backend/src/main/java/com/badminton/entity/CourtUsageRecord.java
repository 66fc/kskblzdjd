package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("court_usage_record")
public class CourtUsageRecord {
    @TableId(type = IdType.AUTO)
    private Long usageId;
    private Long bookingId;
    private Long courtId;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private Integer actualMinutes;
    private Integer billedMinutes;
    private BigDecimal actualFeeAmount;
    private String settlementStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String bookingNo;
    @TableField(exist = false)
    private String courtName;
}
