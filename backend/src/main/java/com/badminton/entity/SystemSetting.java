package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("system_setting")
public class SystemSetting {
    @TableId(type = IdType.INPUT)
    private Integer settingId;
    private LocalDateTime simulatedNow;
    private LocalDateTime baseRealAt;
    private String clockMode;
    private Boolean clockPaused;
    private BigDecimal clockSpeed;
    private Boolean demoMode;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
