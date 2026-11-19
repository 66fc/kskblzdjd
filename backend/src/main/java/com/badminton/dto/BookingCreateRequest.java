package com.badminton.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingCreateRequest {
    /**
     * 仅管理员代客下单时使用；普通会员忽略此字段，强制为本人。
     */
    private Long memberId;

    @NotNull(message = "场地不能为空")
    private Long courtId;

    @NotBlank(message = "开始时间不能为空")
    private String startAt;

    @NotBlank(message = "结束时间不能为空")
    private String endAt;

    /**
     * 客户端幂等 ID，防止重复提交产生重复订单。
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 是否立即支付。默认 true，直接生成 CONFIRMED 预约。
     */
    private Boolean payNow;

    private String remark;
}
