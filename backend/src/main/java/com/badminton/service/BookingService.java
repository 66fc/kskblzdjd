package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.common.DateTimeUtils;
import com.badminton.common.PageResult;
import com.badminton.dto.BookingCreateRequest;
import com.badminton.entity.Booking;
import com.badminton.entity.Court;
import com.badminton.entity.CourtSlot;
import com.badminton.entity.CourtUsageRecord;
import com.badminton.entity.Member;
import com.badminton.entity.MemberLevel;
import com.badminton.mapper.BookingMapper;
import com.badminton.mapper.CourtMapper;
import com.badminton.mapper.CourtSlotMapper;
import com.badminton.mapper.CourtUsageRecordMapper;
import com.badminton.mapper.MemberLevelMapper;
import com.badminton.mapper.MemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookingService {

    private final BookingMapper bookingMapper;
    private final CourtMapper courtMapper;
    private final CourtSlotMapper slotMapper;
    private final CourtUsageRecordMapper usageMapper;
    private final MemberMapper memberMapper;
    private final MemberLevelMapper levelMapper;
    private final CourtService courtService;
    private final MemberService memberService;
    private final SimulatedClockService clock;

    public BookingService(BookingMapper bookingMapper, CourtMapper courtMapper, CourtSlotMapper slotMapper,
                          CourtUsageRecordMapper usageMapper, MemberMapper memberMapper,
                          MemberLevelMapper levelMapper, CourtService courtService,
                          MemberService memberService, SimulatedClockService clock) {
        this.bookingMapper = bookingMapper;
        this.courtMapper = courtMapper;
        this.slotMapper = slotMapper;
        this.usageMapper = usageMapper;
        this.memberMapper = memberMapper;
        this.levelMapper = levelMapper;
        this.courtService = courtService;
        this.memberService = memberService;
        this.clock = clock;
    }

    /**
     * 创建预约。
     * 事务流程：初始化时间段 -> SELECT ... FOR UPDATE 锁定 -> 校验全部空闲
     * -> 服务端计价 -> 写入预约 -> AFTER INSERT 触发器占用时间段。
     */
    @Transactional
    public Booking create(BookingCreateRequest request, Long currentMemberId, boolean admin) {
        Booking existing = bookingMapper.selectOne(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRequestId, request.getRequestId()));
        if (existing != null) {
            return bookingMapper.selectDetail(existing.getBookingId());
        }

        Long memberId = admin && request.getMemberId() != null ? request.getMemberId() : currentMemberId;
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw BizException.notFound("会员不存在");
        }
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new BizException(HttpStatus.FORBIDDEN, "账号已被禁用，无法预约");
        }

        LocalDateTime start = DateTimeUtils.parseDateTime(request.getStartAt());
        LocalDateTime end = DateTimeUtils.parseDateTime(request.getEndAt());
        courtService.validateBookingWindow(start, end);

        Court court = courtMapper.selectById(request.getCourtId());
        if (court == null) {
            throw BizException.notFound("场地不存在");
        }
        courtService.requireOpenCourt(court.getCourtId(), start, end);

        int neededSlots = (int) (Duration.between(start, end).toMinutes() / 30);
        if (slotMapper.countRange(court.getCourtId(), start, end) < neededSlots) {
            courtService.ensureSlotsRange(court.getCourtId(), start, end);
        }

        List<CourtSlot> slots = slotMapper.lockRange(court.getCourtId(), start, end);
        if (slots.size() != neededSlots) {
            throw BizException.conflict("预约失败：时间段尚未完整初始化，请联系管理员");
        }
        for (CourtSlot slot : slots) {
            if (!"AVAILABLE".equals(slot.getSlotStatus()) || slot.getBookingId() != null) {
                throw BizException.conflict("预约失败：所选时间段已被占用，请刷新后重试");
            }
        }

        MemberLevel level = levelMapper.selectById(member.getLevelId());
        BigDecimal discount = level == null ? BigDecimal.ONE : level.getDiscountRate();
        int minutes = (int) Duration.between(start, end).toMinutes();
        BigDecimal original = court.getHourlyRate()
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal payable = original.multiply(discount).setScale(2, RoundingMode.HALF_UP);

        boolean payNow = request.getPayNow() == null || request.getPayNow();
        Booking booking = new Booking();
        booking.setBookingNo(generateBookingNo());
        booking.setRequestId(request.getRequestId());
        booking.setMemberId(memberId);
        booking.setCourtId(court.getCourtId());
        booking.setStartAt(start);
        booking.setEndAt(end);
        booking.setDurationMinutes(minutes);
        booking.setHourlyRateSnapshot(court.getHourlyRate());
        booking.setDiscountSnapshot(discount);
        booking.setOriginalAmount(original);
        booking.setPayableAmount(payable);
        booking.setPaidAmount(payNow ? payable : BigDecimal.ZERO);
        booking.setStatus(payNow ? "CONFIRMED" : "PENDING_PAYMENT");
        booking.setCreatedAt(clock.now());

        try {
            bookingMapper.insert(booking);
        } catch (DuplicateKeyException ex) {
            Booking duplicated = bookingMapper.selectOne(new LambdaQueryWrapper<Booking>()
                    .eq(Booking::getRequestId, request.getRequestId()));
            if (duplicated != null) {
                return bookingMapper.selectDetail(duplicated.getBookingId());
            }
            throw BizException.conflict("预约订单号冲突，请重试");
        }

        return bookingMapper.selectDetail(booking.getBookingId());
    }

    public PageResult<Booking> mine(long page, long size, Long memberId, String status) {
        return mine(page, size, memberId, status, null);
    }

    public PageResult<Booking> mine(long page, long size, Long memberId, String status, LocalDate date) {
        IPage<Booking> result = bookingMapper.selectMemberPage(new Page<>(page, size), memberId, status, date);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Map<String, Long> statusCounts(Long memberId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", 0L);
        for (Map<String, Object> row : bookingMapper.selectStatusCounts(memberId)) {
            String status = String.valueOf(row.get("status"));
            long count = ((Number) row.get("count")).longValue();
            counts.put(status, count);
            counts.put("ALL", counts.get("ALL") + count);
        }
        return counts;
    }

    public PageResult<Booking> adminPage(long page, long size, String keyword, Long memberId,
                                         Long courtId, String status, LocalDate date) {
        IPage<Booking> result = bookingMapper.selectAdminPage(new Page<>(page, size), keyword,
                memberId, courtId, status, date);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public Map<String, Object> detail(Long bookingId) {
        Booking booking = bookingMapper.selectDetail(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        List<CourtSlot> slots = slotMapper.selectRange(booking.getCourtId(), booking.getStartAt(), booking.getEndAt());
        CourtUsageRecord usage = usageMapper.selectOne(new LambdaQueryWrapper<CourtUsageRecord>()
                .eq(CourtUsageRecord::getBookingId, bookingId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("booking", booking);
        result.put("slots", slots);
        result.put("usage", usage);
        return result;
    }

    /**
     * 取消预约：只有 PENDING_PAYMENT / CONFIRMED 且未开始可取消。
     * UPDATE 触发器仅释放本订单占用的时间段。
     */
    @Transactional
    public Booking cancel(Long bookingId, String reason, Long operatorId, boolean admin) {
        Booking booking = bookingMapper.selectForUpdate(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        if (!admin && !booking.getMemberId().equals(operatorId)) {
            throw BizException.forbidden("只能取消自己的预约");
        }
        if (!List.of("PENDING_PAYMENT", "CONFIRMED").contains(booking.getStatus())) {
            throw new BizException(HttpStatus.CONFLICT, "当前状态（" + booking.getStatus() + "）不可取消");
        }
        if (!booking.getStartAt().isAfter(clock.now())) {
            throw new BizException(HttpStatus.CONFLICT, "预约已开始或已结束，不能取消");
        }
        booking.setStatus("CANCELLED");
        booking.setCancelledAt(clock.now());
        booking.setCancelReason(reason == null || reason.isBlank() ? "用户取消" : reason.trim());
        bookingMapper.updateById(booking);
        return bookingMapper.selectDetail(bookingId);
    }

    /**
     * 支付待支付订单：锁定时间段 -> 校验 -> 状态改为 CONFIRMED（触发器占场）。
     */
    @Transactional
    public Booking pay(Long bookingId, Long operatorId, boolean admin) {
        Booking booking = bookingMapper.selectForUpdate(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        if (!admin && !booking.getMemberId().equals(operatorId)) {
            throw BizException.forbidden("只能支付自己的预约");
        }
        if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
            throw new BizException("当前状态不是待支付");
        }
        if (!booking.getStartAt().isAfter(clock.now())) {
            throw new BizException("预约时间已过，无法支付");
        }

        int needed = booking.getDurationMinutes() / 30;
        if (slotMapper.countRange(booking.getCourtId(), booking.getStartAt(), booking.getEndAt()) < needed) {
            courtService.ensureSlotsRange(booking.getCourtId(), booking.getStartAt(), booking.getEndAt());
        }
        List<CourtSlot> slots = slotMapper.lockRange(booking.getCourtId(), booking.getStartAt(), booking.getEndAt());
        if (slots.size() != needed || slots.stream().anyMatch(s ->
                !"AVAILABLE".equals(s.getSlotStatus()) || s.getBookingId() != null)) {
            throw BizException.conflict("支付失败：所选时间段已被占用");
        }

        booking.setStatus("CONFIRMED");
        booking.setPaidAmount(booking.getPayableAmount());
        bookingMapper.updateById(booking);
        return bookingMapper.selectDetail(bookingId);
    }

    /**
     * 签到 / 开始使用：记录真实开始时间，时间段标记为 IN_USE。
     */
    @Transactional
    public Booking checkIn(Long bookingId, Long operatorId) {
        Booking booking = bookingMapper.selectForUpdate(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new BizException("只有已确认的预约可以签到");
        }
        LocalDateTime now = clock.now();
        if (now.isAfter(booking.getEndAt())) {
            throw new BizException("预约已过期，无法签到");
        }
        if (now.isBefore(booking.getStartAt().minusMinutes(30))) {
            throw new BizException("最早可在开始前30分钟签到");
        }
        booking.setStatus("IN_PROGRESS");
        booking.setActualStartAt(now);
        bookingMapper.updateById(booking);
        return bookingMapper.selectDetail(bookingId);
    }

    /**
     * 结束使用：按实际起止时间生成 court_usage_record 并结算。
     * 实际时长与预约时长严格区分，费用按 30 分钟向上取整。
     */
    @Transactional
    public Map<String, Object> checkOut(Long bookingId) {
        Booking booking = bookingMapper.selectForUpdate(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        if (!"IN_PROGRESS".equals(booking.getStatus())) {
            throw new BizException("只有使用中的预约可以结束使用");
        }
        long exists = usageMapper.selectCount(new LambdaQueryWrapper<CourtUsageRecord>()
                .eq(CourtUsageRecord::getBookingId, bookingId));
        if (exists > 0) {
            throw new BizException("该预约已生成使用记录");
        }

        LocalDateTime now = clock.now();
        LocalDateTime start = booking.getActualStartAt() != null ? booking.getActualStartAt() : booking.getStartAt();
        LocalDateTime end = now.isAfter(start) ? now : start.plusMinutes(1);
        int minutes = (int) Duration.between(start, end).toMinutes();
        if (minutes < 1) {
            minutes = 1;
        }
        int billedMinutes = (int) (Math.ceil(minutes / 30.0) * 30);
        BigDecimal fee = booking.getHourlyRateSnapshot()
                .multiply(BigDecimal.valueOf(billedMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .multiply(booking.getDiscountSnapshot())
                .setScale(2, RoundingMode.HALF_UP);

        CourtUsageRecord usage = new CourtUsageRecord();
        usage.setBookingId(bookingId);
        usage.setCourtId(booking.getCourtId());
        usage.setActualStartAt(start);
        usage.setActualEndAt(end);
        usage.setActualMinutes(minutes);
        usage.setBilledMinutes(billedMinutes);
        usage.setActualFeeAmount(fee);
        usage.setSettlementStatus("SETTLED");
        usage.setCreatedAt(now);
        usageMapper.insert(usage);

        booking.setStatus("COMPLETED");
        booking.setActualEndAt(end);
        booking.setPaidAmount(fee);
        bookingMapper.updateById(booking);

        memberService.addConsumptionAndUpgrade(booking.getMemberId(), fee);

        Map<String, Object> result = detail(bookingId);
        result.put("settlement", Map.of(
                "actualMinutes", minutes,
                "billedMinutes", billedMinutes,
                "actualFeeAmount", fee
        ));
        return result;
    }

    @Transactional
    public Booking noShow(Long bookingId, String reason) {
        Booking booking = bookingMapper.selectForUpdate(bookingId);
        if (booking == null) {
            throw BizException.notFound("预约不存在");
        }
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new BizException("只有已确认的预约可标记未到场");
        }
        if (clock.now().isBefore(booking.getStartAt())) {
            throw new BizException("预约尚未开始，不能标记未到场");
        }
        booking.setStatus("NO_SHOW");
        booking.setCancelReason(reason == null || reason.isBlank() ? "超时未到场" : reason.trim());
        bookingMapper.updateById(booking);
        return bookingMapper.selectDetail(bookingId);
    }

    public List<Booking> todayBookings(LocalDate date, int limit) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .ge(Booking::getStartAt, start)
                .lt(Booking::getStartAt, end)
                .orderByAsc(Booking::getStartAt)
                .last("LIMIT " + Math.min(limit, 50)));
    }

    public List<Booking> recentBookings(int limit) {
        return bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .orderByDesc(Booking::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 50)));
    }

    /**
     * 仅用于测试：检查指定时间段内所有 slot 状态。
     */
    public List<CourtSlot> slotsOf(Long courtId, LocalDateTime start, LocalDateTime end) {
        return slotMapper.selectRange(courtId, start, end);
    }

    private String generateBookingNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(100, 1000);
        return "BK" + timestamp + random;
    }
}
