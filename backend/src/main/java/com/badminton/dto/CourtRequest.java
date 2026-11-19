package com.badminton.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtRequest {
    @NotNull(message = "所属场馆不能为空")
    private Long venueId;

    @NotBlank(message = "场地名称不能为空")
    @Size(max = 50, message = "场地名称最长50个字符")
    private String courtName;

    @NotBlank(message = "场地编号不能为空")
    @Size(max = 20, message = "场地编号最长20个字符")
    private String courtNumber;

    @NotNull(message = "每小时单价不能为空")
    @DecimalMin(value = "0.00", message = "单价不能为负数")
    private BigDecimal hourlyRate;

    @NotBlank(message = "运营状态不能为空")
    private String operationalStatus;

    @Size(max = 255, message = "场地介绍最长255个字符")
    private String description;
}
