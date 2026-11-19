package com.badminton.controller;

import com.badminton.aspect.AdminLog;
import com.badminton.common.ApiResponse;
import com.badminton.common.PageResult;
import com.badminton.entity.Booking;
import com.badminton.security.CurrentUser;
import com.badminton.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
@Tag(name = "后台-预约管理", description = "预约查询、取消、签到、结束使用、未到场")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(summary = "预约列表（支持订单号/会员/场地/日期/状态筛选）")
    public ApiResponse<PageResult<Booking>> list(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long memberId,
                                                 @RequestParam(required = false) Long courtId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(bookingService.adminPage(page, size, keyword, memberId, courtId, status, date));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "预约详情（含时间段与实际使用记录）")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long bookingId) {
        return ApiResponse.ok(bookingService.detail(bookingId));
    }

    @PatchMapping("/{bookingId}/cancel")
    @AdminLog(module = "BOOKING", action = "CANCEL_BOOKING", targetType = "BOOKING", detail = "管理员取消预约")
    @Operation(summary = "取消预约")
    public ApiResponse<Booking> cancel(@PathVariable Long bookingId,
                                       @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok("预约已取消", bookingService.cancel(bookingId, reason, CurrentUser.id(), true));
    }

    @PatchMapping("/{bookingId}/check-in")
    @AdminLog(module = "BOOKING", action = "CHECK_IN", targetType = "BOOKING", detail = "预约签到")
    @Operation(summary = "签到 / 开始使用")
    public ApiResponse<Booking> checkIn(@PathVariable Long bookingId) {
        return ApiResponse.ok("签到成功", bookingService.checkIn(bookingId, CurrentUser.id()));
    }

    @PatchMapping("/{bookingId}/check-out")
    @AdminLog(module = "BOOKING", action = "CHECK_OUT", targetType = "BOOKING", detail = "结束使用并结算")
    @Operation(summary = "结束使用（生成实际使用记录并结算）")
    public ApiResponse<Map<String, Object>> checkOut(@PathVariable Long bookingId) {
        return ApiResponse.ok("已结束使用并完成结算", bookingService.checkOut(bookingId));
    }

    @PatchMapping("/{bookingId}/no-show")
    @AdminLog(module = "BOOKING", action = "NO_SHOW", targetType = "BOOKING", detail = "标记未到场")
    @Operation(summary = "标记未到场（释放时间段）")
    public ApiResponse<Booking> noShow(@PathVariable Long bookingId,
                                       @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok("已标记未到场", bookingService.noShow(bookingId, reason));
    }
}
