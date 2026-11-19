package com.badminton.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("venue")
public class Venue {
    @TableId(type = IdType.AUTO)
    private Long venueId;
    private String venueName;
    private String address;
    private String phone;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
