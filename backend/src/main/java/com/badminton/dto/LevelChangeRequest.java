package com.badminton.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LevelChangeRequest {
    @NotNull(message = "目标等级不能为空")
    private Long levelId;

    @NotBlank(message = "修改原因不能为空")
    @Size(max = 200, message = "修改原因最长200个字符")
    private String reason;
}
