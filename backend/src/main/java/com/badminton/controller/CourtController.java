package com.badminton.controller;

import com.badminton.common.ApiResponse;
import com.badminton.dto.AvailabilityRow;
import com.badminton.entity.Court;
import com.badminton.entity.CourtSlot;
import com.badminton.entity.Venue;
import com.badminton.mapper.VenueMapper;
import com.badminton.service.CourtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "场地与可预约查询", description = "公开场地列表、可预约查询、场地时间段")
public class CourtController {

    private final CourtService courtService;
    private final VenueMapper venueMapper;

    public CourtController(CourtService courtService, VenueMapper venueMapper) {
        this.courtService = courtService;
        this.venueMapper = venueMapper;
    }

    @GetMapping("/venues")
    @Operation(summary = "场馆列表")
    public ApiResponse<List<Venue>> venues() {
        return ApiResponse.ok(venueMapper.selectList(null));
    }

    @GetMapping("/courts")
    @Operation(summary = "场地列表")
    public ApiResponse<List<Court>> courts(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long venueId) {
        return ApiResponse.ok(courtService.list(status, venueId));
    }

    @GetMapping("/courts/{courtId}")
    @Operation(summary = "场地详情")
    public ApiResponse<Court> court(@PathVariable Long courtId) {
        return ApiResponse.ok(courtService.detail(courtId));
    }

    @GetMapping("/courts/{courtId}/slots")
    @Operation(summary = "场地某日时间段（会员端日历）")
    public ApiResponse<List<CourtSlot>> slots(@PathVariable Long courtId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(courtService.slots(courtId, date));
    }

    @GetMapping("/courts/availability")
    @Operation(summary = "查询区间内完整空闲的场地（视图 v_available_court_slots）")
    public ApiResponse<List<AvailabilityRow>> availability(@RequestParam String startAt,
                                                           @RequestParam String endAt) {
        return ApiResponse.ok(courtService.availability(startAt, endAt));
    }

    @GetMapping("/courts/overview")
    @Operation(summary = "某日场地总览（时间段数量与当前状态）")
    public ApiResponse<List<com.badminton.dto.CourtOverviewRow>> overview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(courtService.overview(date));
    }
}
