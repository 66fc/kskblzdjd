package com.badminton.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SalesStatisticsResponse {
    private LocalDate from;
    private LocalDate to;
    private String groupBy;
    private BigDecimal rentalTotal;
    private BigDecimal productTotal;
    private BigDecimal grandTotal;
    private List<DailyRevenueRow> rentalSeries;
    private List<DailyRevenueRow> productSeries;
    private List<MonthlyPoint> monthlySeries;

    @Data
    public static class MonthlyPoint {
        private String month;
        private BigDecimal rental;
        private BigDecimal product;
        private BigDecimal total;
    }
}
