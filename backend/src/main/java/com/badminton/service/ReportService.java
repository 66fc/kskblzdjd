package com.badminton.service;

import com.badminton.common.BizException;
import com.badminton.dto.CourtDailyStat;
import com.badminton.dto.CourtDailySummary;
import com.badminton.dto.CourtUsageRow;
import com.badminton.dto.DailyReportResponse;
import com.badminton.dto.DailyRevenueRow;
import com.badminton.dto.DashboardCounts;
import com.badminton.dto.DashboardResponse;
import com.badminton.entity.Booking;
import com.badminton.entity.Court;
import com.badminton.entity.Venue;
import com.badminton.mapper.BookingMapper;
import com.badminton.mapper.CourtMapper;
import com.badminton.mapper.ReportMapper;
import com.badminton.mapper.VenueMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportMapper reportMapper;
    private final CourtMapper courtMapper;
    private final VenueMapper venueMapper;
    private final BookingMapper bookingMapper;
    private final SalesService salesService;
    private final SimulatedClockService clock;

    public ReportService(ReportMapper reportMapper, CourtMapper courtMapper, VenueMapper venueMapper,
                         BookingMapper bookingMapper, SalesService salesService,
                         SimulatedClockService clock) {
        this.reportMapper = reportMapper;
        this.courtMapper = courtMapper;
        this.venueMapper = venueMapper;
        this.bookingMapper = bookingMapper;
        this.salesService = salesService;
        this.clock = clock;
    }

    /**
     * 日使用统计：逐场地调用 sp_daily_court_stats_by_court，
     * 汇总调用 sp_daily_court_stats_all，全部来自数据库存储过程。
     */
    public DailyReportResponse daily(LocalDate date) {
        if (date == null) {
            throw new BizException("统计日期不能为空");
        }
        List<Court> courts = courtMapper.selectList(new LambdaQueryWrapper<Court>()
                .orderByAsc(Court::getCourtId));
        List<CourtDailyStat> stats = new ArrayList<>();
        for (Court court : courts) {
            CourtDailyStat stat = reportMapper.callDailyCourtStat(date, court.getCourtId());
            if (stat == null) {
                stat = new CourtDailyStat();
                stat.setStatDate(date);
                stat.setCourtId(court.getCourtId());
                stat.setCourtName(court.getCourtName());
                stat.setTotalActualMinutes(0L);
                stat.setTotalActualHours(BigDecimal.ZERO);
                stat.setTotalSalesAmount(BigDecimal.ZERO);
                stat.setUsageRecordCount(0);
            }
            stats.add(stat);
        }
        CourtDailySummary total = reportMapper.callDailySummary(date);
        if (total == null) {
            total = new CourtDailySummary();
            total.setStatDate(date);
            total.setTotalActualMinutes(0L);
            total.setTotalActualHours(BigDecimal.ZERO);
            total.setTotalSalesAmount(BigDecimal.ZERO);
            total.setUsedCourtCount(0);
            total.setUsageRecordCount(0);
        }
        DailyReportResponse response = new DailyReportResponse();
        response.setDate(date);
        response.setCourts(stats);
        response.setTotal(total);
        response.setProcedure("sp_daily_court_stats_by_court / sp_daily_court_stats_all");
        return response;
    }

    public CourtDailyStat dailyCourt(LocalDate date, Long courtId) {
        if (date == null) {
            throw new BizException("统计日期不能为空");
        }
        CourtDailyStat stat = reportMapper.callDailyCourtStat(date, courtId);
        if (stat == null) {
            Court court = courtMapper.selectById(courtId);
            if (court == null) {
                throw BizException.notFound("场地不存在");
            }
            stat = new CourtDailyStat();
            stat.setStatDate(date);
            stat.setCourtId(courtId);
            stat.setCourtName(court.getCourtName());
            stat.setTotalActualMinutes(0L);
            stat.setTotalActualHours(BigDecimal.ZERO);
            stat.setTotalSalesAmount(BigDecimal.ZERO);
            stat.setUsageRecordCount(0);
        }
        return stat;
    }

    public DashboardResponse dashboard(LocalDate date) {
        LocalDateTime now = clock.now();
        LocalDate businessDate = date == null ? now.toLocalDate() : date;
        LocalDateTime dayStart = businessDate.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        DashboardCounts counts = reportMapper.selectDashboardCounts(businessDate, dayStart, dayEnd, now);
        if (counts == null) {
            counts = new DashboardCounts();
        }
        if (!businessDate.equals(now.toLocalDate())) {
            counts.setFreeCourtsNow(0L);
            counts.setInUseCourts(0L);
        }

        CourtDailySummary summary = reportMapper.callDailySummary(businessDate);
        if (summary == null) {
            summary = new CourtDailySummary();
            summary.setStatDate(businessDate);
            summary.setTotalActualMinutes(0L);
            summary.setTotalActualHours(BigDecimal.ZERO);
            summary.setTotalSalesAmount(BigDecimal.ZERO);
            summary.setUsedCourtCount(0);
            summary.setUsageRecordCount(0);
        }

        DashboardResponse response = new DashboardResponse();
        response.setBusinessDate(businessDate);
        response.setSimulatedNow(now);
        response.setCounts(counts);
        response.setTodaySummary(summary);
        response.setTodayRentalRevenue(summary.getTotalSalesAmount());
        response.setTodayProductRevenue(salesService.productRevenue(businessDate));
        response.setTodayMinutes(summary.getTotalActualMinutes());
        response.setTodayHours(summary.getTotalActualHours());

        LocalDate trendFrom = businessDate.minusDays(13);
        List<DailyRevenueRow> rental = reportMapper.selectDailyRentalTrend(trendFrom, businessDate);
        List<DailyRevenueRow> product = reportMapper.selectDailyProductTrend(trendFrom, businessDate);
        Map<LocalDate, DailyRevenueRow> productMap = product.stream()
                .collect(Collectors.toMap(DailyRevenueRow::getStatDate, Function.identity(), (a, b) -> a));
        List<DashboardResponse.TrendPoint> trend = new ArrayList<>();
        for (DailyRevenueRow row : rental) {
            DashboardResponse.TrendPoint point = new DashboardResponse.TrendPoint();
            point.setDate(row.getStatDate());
            point.setRental(row.getAmount());
            DailyRevenueRow pr = productMap.get(row.getStatDate());
            point.setProduct(pr == null ? BigDecimal.ZERO : pr.getAmount());
            point.setTotal(point.getRental().add(point.getProduct()));
            trend.add(point);
        }
        response.setTrend(trend);
        response.setCourtUsage(buildCourtUsage(dayStart, dayEnd));

        response.setTodayBookings(bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .ge(Booking::getStartAt, dayStart)
                .lt(Booking::getStartAt, dayEnd)
                .orderByAsc(Booking::getStartAt)
                .last("LIMIT 8")));
        return response;
    }

    private List<DashboardResponse.CourtUsagePoint> buildCourtUsage(LocalDateTime dayStart, LocalDateTime dayEnd) {
        List<CourtUsageRow> rows = reportMapper.selectCourtUsageOfDay(dayStart, dayEnd);
        Venue venue = venueMapper.selectList(new LambdaQueryWrapper<Venue>()
                .eq(Venue::getStatus, "ACTIVE").last("LIMIT 1")).stream().findFirst().orElse(null);
        long capacity = 930;
        if (venue != null) {
            capacity = Duration.between(venue.getOpeningTime(), venue.getClosingTime()).toMinutes();
        }
        List<DashboardResponse.CourtUsagePoint> points = new ArrayList<>();
        for (CourtUsageRow row : rows) {
            DashboardResponse.CourtUsagePoint point = new DashboardResponse.CourtUsagePoint();
            point.setCourtId(row.getCourtId());
            point.setCourtName(row.getCourtName());
            point.setMinutes(row.getMinutes());
            point.setAmount(row.getAmount());
            BigDecimal rate = capacity <= 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(row.getMinutes() == null ? 0 : row.getMinutes())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(capacity), 1, RoundingMode.HALF_UP);
            point.setUsageRate(rate);
            points.add(point);
        }
        return points;
    }

    /**
     * 周/月统计：按日期区间汇总场地租赁收入、商品收入、预约数、实际使用时长。
     */
    public Map<String, Object> overview(LocalDate date, String range) {
        LocalDate end = date == null ? clock.today() : date;
        String type = range == null ? "day" : range.toLowerCase();
        LocalDate from = switch (type) {
            case "week" -> end.minusDays(6);
            case "month" -> end.withDayOfMonth(1);
            default -> end;
        };

        List<DailyRevenueRow> rental = reportMapper.selectDailyRentalTrend(from, end);
        List<DailyRevenueRow> product = reportMapper.selectDailyProductTrend(from, end);
        BigDecimal rentalTotal = rental.stream().map(DailyRevenueRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productTotal = product.stream().map(DailyRevenueRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long minutes = rental.stream().mapToLong(r -> r.getMinutes() == null ? 0 : r.getMinutes()).sum();
        long bookings = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .ge(Booking::getStartAt, from.atStartOfDay())
                .lt(Booking::getStartAt, end.plusDays(1).atStartOfDay()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", type);
        result.put("from", from);
        result.put("to", end);
        result.put("rentalRevenue", rentalTotal);
        result.put("productRevenue", productTotal);
        result.put("totalRevenue", rentalTotal.add(productTotal));
        result.put("actualMinutes", minutes);
        result.put("actualHours", BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
        result.put("bookingCount", bookings);
        result.put("rentalSeries", rental);
        result.put("productSeries", product);
        return result;
    }
}
