package com.badminton.controller;

import com.badminton.common.ApiResponse;
import com.badminton.common.PageResult;
import com.badminton.dto.ConsumptionRecord;
import com.badminton.entity.Booking;
import com.badminton.mapper.MemberMapper;
import com.badminton.security.CurrentUser;
import com.badminton.service.BookingService;
import com.badminton.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * C 端用户中心：只允许操作当前登录用户自己的数据。
 */
@RestController
@RequestMapping("/api")
@Tag(name = "C端-用户中心", description = "我的预约、预约状态统计、消费记录、会员资料")
public class UserCenterController {

    private final BookingService bookingService;
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    public UserCenterController(BookingService bookingService, MemberService memberService,
                                MemberMapper memberMapper) {
        this.bookingService = bookingService;
        this.memberService = memberService;
        this.memberMapper = memberMapper;
    }

    @GetMapping("/users/me/bookings")
    @Operation(summary = "我的预约（支持状态与日期筛选）")
    public ApiResponse<PageResult<Booking>> myBookings(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(bookingService.mine(page, size, CurrentUser.id(), status, date));
    }

    @GetMapping("/users/me/booking-stats")
    @Operation(summary = "我的预约状态数量")
    public ApiResponse<Map<String, Long>> bookingStats() {
        return ApiResponse.ok(bookingService.statusCounts(CurrentUser.id()));
    }

    @GetMapping("/users/me/orders")
    @Operation(summary = "我的消费记录（场地租赁 + 商品零售）")
    public ApiResponse<Map<String, Object>> myOrders() {
        Long memberId = CurrentUser.id();
        List<ConsumptionRecord> records = memberMapper.selectConsumptionRecords(memberId);
        BigDecimal rental = records.stream()
                .filter(r -> "COURT_RENTAL".equals(r.getConsumeType()))
                .map(ConsumptionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal product = records.stream()
                .filter(r -> "PRODUCT".equals(r.getConsumeType()))
                .map(ConsumptionRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("rentalAmount", rental);
        result.put("productAmount", product);
        result.put("totalAmount", rental.add(product));
        result.put("count", records.size());
        return ApiResponse.ok(result);
    }

    @GetMapping("/members/me")
    @Operation(summary = "我的会员资料、预约统计、消费与调级历史")
    public ApiResponse<Map<String, Object>> myProfile() {
        return ApiResponse.ok(memberService.detail(CurrentUser.id()));
    }
}
