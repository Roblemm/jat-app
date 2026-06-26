package com.jat.app.dto.dashboard;

import com.jat.app.dto.goal.GoalResponse;
import com.jat.app.dto.task.TaskResponse;

import java.time.LocalDate;
import java.util.List;

// A read model for the Today screen; it composes existing task and goal data without storing dashboard state.
public record TodayDashboardResponse(
        LocalDate date,
        List<TaskResponse> tasksDueToday,
        List<TaskResponse> overdueTasks,
        List<TaskResponse> scheduledBlocks,
        List<GoalResponse> activeGoals,
        DashboardCounts counts
) {
}
