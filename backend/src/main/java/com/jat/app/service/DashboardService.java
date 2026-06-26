package com.jat.app.service;

import com.jat.app.dto.dashboard.DashboardCounts;
import com.jat.app.dto.dashboard.TodayDashboardResponse;
import com.jat.app.dto.goal.GoalResponse;
import com.jat.app.dto.task.TaskResponse;
import com.jat.app.entity.Goal;
import com.jat.app.entity.Project;
import com.jat.app.entity.Task;
import com.jat.app.repository.GoalRepository;
import com.jat.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;

    @Transactional(readOnly = true)
    public TodayDashboardResponse getToday(LocalDate date, UUID areaId) {
        // Local-first date boundaries should match the laptop's local timezone, not UTC midnight.
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<TaskResponse> tasksDueToday = taskRepository.findDashboardDueTasks(start, end, areaId).stream()
                .map(this::toTaskResponse)
                .toList();
        List<TaskResponse> overdueTasks = taskRepository.findDashboardOverdueTasks(start, areaId).stream()
                .map(this::toTaskResponse)
                .toList();
        List<TaskResponse> scheduledBlocks = taskRepository.findDashboardScheduledTasks(start, end, areaId).stream()
                .map(this::toTaskResponse)
                .toList();
        List<GoalResponse> activeGoals = goalRepository.findDashboardActiveGoals(areaId).stream()
                .map(this::toGoalResponse)
                .toList();

        DashboardCounts counts = new DashboardCounts(
                tasksDueToday.size(),
                overdueTasks.size(),
                scheduledBlocks.size(),
                activeGoals.size()
        );

        return new TodayDashboardResponse(date, tasksDueToday, overdueTasks, scheduledBlocks, activeGoals, counts);
    }

    private TaskResponse toTaskResponse(Task task) {
        Project project = task.getProject();
        Goal goal = task.getGoal();

        // Dashboard responses reuse the same task shape as /api/tasks to keep frontend models consistent.
        return new TaskResponse(
                task.getId(),
                task.getArea().getId(),
                task.getArea().getName(),
                project == null ? null : project.getId(),
                project == null ? null : project.getName(),
                goal == null ? null : goal.getId(),
                goal == null ? null : goal.getTitle(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskType(),
                task.getStatus(),
                task.getPriority(),
                task.getRecurrence(),
                task.getDueAt(),
                task.getRemindAt(),
                task.getScheduledStart(),
                task.getScheduledEnd(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private GoalResponse toGoalResponse(Goal goal) {
        Project project = goal.getProject();

        // Dashboard responses reuse the same goal shape as /api/goals for predictable API contracts.
        return new GoalResponse(
                goal.getId(),
                goal.getArea().getId(),
                goal.getArea().getName(),
                project == null ? null : project.getId(),
                project == null ? null : project.getName(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getGoalType(),
                goal.getStatus(),
                goal.getRecurrence(),
                goal.getTargetValue(),
                goal.getUnit(),
                goal.getTargetDate(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
