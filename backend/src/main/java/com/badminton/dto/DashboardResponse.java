package com.badminton.dto;

import com.badminton.entity.Booking;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardResponse {
    private LocalDate businessDate;
    private LocalDateTime simulatedNow;
    private DashboardCounts counts;
    private CourtDailySummary todaySummary;
    private BigDecimal todayRentalRevenue;
    private BigDecimal todayProductRevenue;
    private Long todayMinutes;
    private BigDecimal todayHours;
    private List<TrendPoint> trend;
    private List<CourtUsagePoint> courtUsage;
    private List<Booking> todayBookings;

    @Data
    public static class TrendPoint {
        private LocalDate date;
        private BigDecimal rental;
        private BigDecimal product;
        private BigDecimal total;
    }

    @Data
    public static class CourtUsagePoint {
        private Long courtId;
        private String courtName;
        private Long minutes;
        private BigDecimal amount;
        private BigDecimal usageRate;
    }
}
