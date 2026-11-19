package com.badminton.dto;

import lombok.Data;

@Data
public class DashboardCounts {
    private Long totalCourts;
    private Long openCourts;
    private Long freeCourtsNow;
    private Long inUseCourts;
    private Long bookedSlotsToday;
    private Long totalMembers;
    private Long todayBookings;
    private Long todayCompletedBookings;
    private Long pendingBookings;
}
