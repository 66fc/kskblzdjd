package com.badminton.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyReportResponse {
    private LocalDate date;
    private List<CourtDailyStat> courts;
    private CourtDailySummary total;
    private String procedure;
}
