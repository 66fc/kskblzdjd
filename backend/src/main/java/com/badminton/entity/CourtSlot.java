package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("court_slot")
public class CourtSlot {
    @TableId(type = IdType.AUTO)
    private Long slotId;
    private Long courtId;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private String slotStatus;
    private Long bookingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
