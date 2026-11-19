package com.badminton.controller;

import com.badminton.common.ApiResponse;
import com.badminton.common.BizException;
import com.badminton.common.PageResult;
import com.badminton.dto.BookingCreateRequest;
import com.badminton.entity.Booking;
import com.badminton.security.CurrentUser;
import com.badminton.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "会员预约", description = "创建预约、我的预约、取消与支付")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "创建预约（事务 + 行锁 + 触发器占场）")
    public ApiResponse<Booking> create(@Valid @RequestBody BookingCreateRequest request) {
        boolean admin = CurrentUser.isAdmin();
        return ApiResponse.ok("预约成功", bookingService.create(request, CurrentUser.id(), admin));
    }

    @GetMapping("/mine")
    @Operation(summary = "我的预约")
    public ApiResponse<PageResult<Booking>> mine(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.ok(bookingService.mine(page, size, CurrentUser.id(), status));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "预约详情")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long bookingId) {
        Map<String, Object> detail = bookingService.detail(bookingId);
        Booking booking = (Booking) detail.get("booking");
        if (!CurrentUser.isAdmin() && !booking.getMemberId().equals(CurrentUser.id())) {
            throw BizException.forbidden("只能查看自己的预约");
        }
        return ApiResponse.ok(detail);
    }

    @PatchMapping("/{bookingId}/cancel")
    @Operation(summary = "取消预约")
    public ApiResponse<Booking> cancel(@PathVariable Long bookingId,
                                       @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok("预约已取消", bookingService.cancel(bookingId, reason,
                CurrentUser.id(), CurrentUser.isAdmin()));
    }

    @PatchMapping("/{bookingId}/pay")
    @Operation(summary = "支付待支付预约")
    public ApiResponse<Booking> pay(@PathVariable Long bookingId) {
        return ApiResponse.ok("支付成功", bookingService.pay(bookingId, CurrentUser.id(), CurrentUser.isAdmin()));
    }
}
