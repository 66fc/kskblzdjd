package com.badminton.mapper;

import com.badminton.dto.CourtDailyStat;
import com.badminton.dto.CourtDailySummary;
import com.badminton.dto.CourtUsageRow;
import com.badminton.dto.DailyRevenueRow;
import com.badminton.dto.DashboardCounts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReportMapper {

    /**
     * 真实调用存储过程：单一场地日统计。
     */
    @Select("{CALL sp_daily_court_stats_by_court(#{date}, #{courtId})}")
    CourtDailyStat callDailyCourtStat(@Param("date") LocalDate date, @Param("courtId") Long courtId);

    /**
     * 真实调用存储过程：全部场地日汇总。
     */
    @Select("{CALL sp_daily_court_stats_all(#{date})}")
    CourtDailySummary callDailySummary(@Param("date") LocalDate date);

    List<DailyRevenueRow> selectDailyRentalTrend(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<DailyRevenueRow> selectDailyProductTrend(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<CourtUsageRow> selectCourtUsageOfDay(@Param("dayStart") LocalDateTime dayStart,
                                             @Param("dayEnd") LocalDateTime dayEnd);

    DashboardCounts selectDashboardCounts(@Param("date") LocalDate date,
                                          @Param("dayStart") LocalDateTime dayStart,
                                          @Param("dayEnd") LocalDateTime dayEnd,
                                          @Param("now") LocalDateTime now);
}
