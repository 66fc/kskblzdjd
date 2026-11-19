package com.badminton.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SlotGenerateRequest {
    @NotNull(message = "开始日期不能为空")
    private LocalDate from;

    @NotNull(message = "结束日期不能为空")
    private LocalDate to;
}
