package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("member")
public class Member {
    @TableId(type = IdType.AUTO)
    private Long memberId;
    private String username;
    @JsonIgnore
    private String passwordHash;
    private String nickname;
    private String phone;
    private String avatar;
    private String role;
    private Long levelId;
    private BigDecimal totalConsumption;
    private String status;
    private Boolean levelManuallyAdjusted;
    private LocalDateTime manualAdjustedAt;
    private Long manualAdjustedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String levelName;
    @TableField(exist = false)
    private BigDecimal discountRate;
}
