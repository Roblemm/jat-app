package com.jat.app.dto.dashboard;

// Counts mirror the list sizes so the frontend can render summary chips without recalculating them.
public record DashboardCounts(
        int dueToday,
        int overdue,
        int scheduled,
        int activeGoals
) {
}
