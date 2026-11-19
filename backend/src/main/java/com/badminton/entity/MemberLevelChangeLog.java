package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("member_level_change_log")
public class MemberLevelChangeLog {
    @TableId(type = IdType.AUTO)
    private Long changeId;
    private Long memberId;
    private Long oldLevelId;
    private Long newLevelId;
    private Long operatorId;
    private String changeType;
    private String reason;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String memberNickname;
    @TableField(exist = false)
    private String oldLevelName;
    @TableField(exist = false)
    private String newLevelName;
    @TableField(exist = false)
    private String operatorName;
}
