package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.common.DateTimeUtils;
import com.badminton.common.PageResult;
import com.badminton.dto.AvailabilityRow;
import com.badminton.dto.CourtOverviewRow;
import com.badminton.dto.CourtRequest;
import com.badminton.entity.Court;
import com.badminton.entity.CourtSlot;
import com.badminton.entity.CourtUsageRecord;
import com.badminton.entity.Venue;
import com.badminton.mapper.BookingMapper;
import com.badminton.mapper.CourtMapper;
import com.badminton.mapper.CourtSlotMapper;
import com.badminton.mapper.CourtUsageRecordMapper;
import com.badminton.mapper.VenueMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CourtService {

    private final CourtMapper courtMapper;
    private final VenueMapper venueMapper;
    private final CourtSlotMapper courtSlotMapper;
    private final BookingMapper bookingMapper;
    private final CourtUsageRecordMapper usageRecordMapper;
    private final SimulatedClockService clock;

    public CourtService(CourtMapper courtMapper, VenueMapper venueMapper, CourtSlotMapper courtSlotMapper,
                        BookingMapper bookingMapper, CourtUsageRecordMapper usageRecordMapper,
                        SimulatedClockService clock) {
        this.courtMapper = courtMapper;
        this.venueMapper = venueMapper;
        this.courtSlotMapper = courtSlotMapper;
        this.bookingMapper = bookingMapper;
        this.usageRecordMapper = usageRecordMapper;
        this.clock = clock;
    }

    public List<Court> list(String status, Long venueId) {
        return courtMapper.selectWithVenue(status, venueId);
    }

    public Court detail(Long courtId) {
        Court court = courtMapper.selectDetail(courtId);
        if (court == null) {
            throw BizException.notFound("场地不存在");
        }
        return court;
    }

    /**
     * C 端首页：某日各场地时间段汇总与当前状态。
     */
    public List<CourtOverviewRow> overview(LocalDate date) {
        LocalDate target = date == null ? clock.today() : date;
        return courtMapper.selectOverview(target, clock.now());
    }

    @Transactional
    public Court create(CourtRequest request) {
        Venue venue = venueMapper.selectById(request.getVenueId());
        if (venue == null) {
            throw BizException.notFound("场馆不存在");
        }
        validateStatus(request.getOperationalStatus());
        Court court = new Court();
        apply(court, request);
        courtMapper.insert(court);
        ensureSlots(court.getCourtId(), clock.today(), clock.today().plusDays(30));
        return detail(court.getCourtId());
    }

    @Transactional
    public Court update(Long courtId, CourtRequest request) {
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw BizException.notFound("场地不存在");
        }
        validateStatus(request.getOperationalStatus());
        apply(court, request);
        courtMapper.updateById(court);
        return detail(courtId);
    }

    /**
     * 删除场地：有历史预约/使用记录时改为停用（逻辑删除），保护业务数据。
     */
    @Transactional
    public String delete(Long courtId) {
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw BizException.notFound("场地不存在");
        }
        long bookings = bookingMapper.selectCount(new LambdaQueryWrapper<com.badminton.entity.Booking>()
                .eq(com.badminton.entity.Booking::getCourtId, courtId));
        long usages = usageRecordMapper.selectCount(new LambdaQueryWrapper<com.badminton.entity.CourtUsageRecord>()
                .eq(com.badminton.entity.CourtUsageRecord::getCourtId, courtId));
        if (bookings > 0 || usages > 0) {
            court.setOperationalStatus("DISABLED");
            courtMapper.updateById(court);
            return "该场地存在 " + bookings + " 条预约记录、" + usages + " 条使用记录，已停用（逻辑删除）以保护历史数据";
        }
        courtMapper.deleteById(courtId);
        return "场地已删除";
    }

    public List<CourtSlot> slots(Long courtId, LocalDate date) {
        courtMapper.selectById(courtId);
        ensureSlots(courtId, date, date);
        return courtSlotMapper.selectByCourtAndDate(courtId, date);
    }

    /**
     * 生成指定日期范围内的时间段（后台维护入口，调用数据库存储过程）。
     */
    public Map<String, Object> generateSlots(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BizException("结束日期不能早于开始日期");
        }
        if (Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() > 90) {
            throw new BizException("单次最多生成90天");
        }
        return courtSlotMapper.callGenerateSlots(from, to);
    }

    /**
     * 确保某场地某天的时间段已初始化（幂等）。
     */
    public void ensureSlots(Long courtId, LocalDate from, LocalDate to) {
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw BizException.notFound("场地不存在");
        }
        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null) {
            throw BizException.notFound("场馆不存在");
        }
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<LocalDateTime> starts = new ArrayList<>();
            LocalDateTime cursor = LocalDateTime.of(date, venue.getOpeningTime());
            LocalDateTime closing = LocalDateTime.of(date, venue.getClosingTime());
            while (!cursor.plusMinutes(30).isAfter(closing)) {
                starts.add(cursor);
                cursor = cursor.plusMinutes(30);
            }
            if (!starts.isEmpty()) {
                courtSlotMapper.insertIgnoreSlots(courtId, starts);
            }
        }
    }

    /**
     * 场地历史使用记录。
     */
    public PageResult<CourtUsageRecord> usageHistory(Long courtId, long page, long size,
                                                    LocalDate from, LocalDate to) {
        IPage<CourtUsageRecord> result = usageRecordMapper.selectPageWithNames(new Page<>(page, size),
                courtId, null, from, to);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 仅补齐指定预约区间内缺失的时间段，避免并发时对整日大量行做 INSERT IGNORE。
     */
    public void ensureSlotsRange(Long courtId, LocalDateTime start, LocalDateTime end) {
        List<LocalDateTime> starts = new ArrayList<>();
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            starts.add(cursor);
            cursor = cursor.plusMinutes(30);
        }
        if (!starts.isEmpty()) {
            courtSlotMapper.insertIgnoreSlots(courtId, starts);
        }
    }

    /**
     * 可预约场地查询：真实查询数据库视图 v_available_court_slots。
     */
    public List<AvailabilityRow> availability(String startText, String endText) {
        LocalDateTime start = DateTimeUtils.parseDateTime(startText);
        LocalDateTime end = DateTimeUtils.parseDateTime(endText);
        validateBookingWindow(start, end);
        int needed = (int) (Duration.between(start, end).toMinutes() / 30);
        return courtSlotMapper.selectAvailableCourts(start, end, needed);
    }

    void validateBookingWindow(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BizException("结束时间必须晚于开始时间");
        }
        if (start.getSecond() != 0 || end.getSecond() != 0
                || start.getMinute() % 30 != 0 || end.getMinute() % 30 != 0) {
            throw new BizException("预约时间必须对齐到30分钟");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 30) {
            throw new BizException("预约时长至少30分钟");
        }
        if (minutes > 360) {
            throw new BizException("单次预约最长6小时");
        }
        if (start.isBefore(clock.now())) {
            throw new BizException("不能预约过去的时间段");
        }
        if (Duration.between(clock.now(), start).toDays() > 30) {
            throw new BizException("最多可预约未来30天内的场地");
        }
    }

    private void apply(Court court, CourtRequest request) {
        court.setVenueId(request.getVenueId());
        court.setCourtName(request.getCourtName().trim());
        court.setCourtNumber(request.getCourtNumber().trim());
        court.setHourlyRate(request.getHourlyRate());
        court.setOperationalStatus(request.getOperationalStatus());
        court.setDescription(request.getDescription());
    }

    private void validateStatus(String status) {
        if (status == null || !List.of("OPEN", "MAINTENANCE", "DISABLED").contains(status)) {
            throw new BizException("运营状态只能是 OPEN / MAINTENANCE / DISABLED");
        }
    }

    /**
     * 校验某场地某时间段是否在营业时间内。
     */
    public Venue requireOpenCourt(Long courtId, LocalDateTime start, LocalDateTime end) {
        Court court = courtMapper.selectById(courtId);
        if (court == null) {
            throw BizException.notFound("场地不存在");
        }
        if (!"OPEN".equals(court.getOperationalStatus())) {
            throw new BizException(HttpStatus.CONFLICT, "场地当前不可预约（维护中或已停用）");
        }
        Venue venue = venueMapper.selectById(court.getVenueId());
        if (venue == null || !"ACTIVE".equals(venue.getStatus())) {
            throw new BizException(HttpStatus.CONFLICT, "场馆暂停营业");
        }
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        if (startTime.isBefore(venue.getOpeningTime()) || endTime.isAfter(venue.getClosingTime())
                || !start.toLocalDate().equals(end.toLocalDate())) {
            throw new BizException("预约时间需在营业时间 " + venue.getOpeningTime() + " - "
                    + venue.getClosingTime() + " 内，且不能跨天");
        }
        return venue;
    }
}
