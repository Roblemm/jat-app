package com.jat.app.service;

import com.jat.app.entity.Area;
import com.jat.app.entity.Goal;
import com.jat.app.entity.GoalType;
import com.jat.app.entity.Recurrence;
import com.jat.app.entity.Task;
import com.jat.app.entity.TaskPriority;
import com.jat.app.entity.TaskType;
import com.jat.app.repository.GoalRepository;
import com.jat.app.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getTodayCombinesTasksGoalsAndCountsForDate() {
        // The Today view is a read model: it summarizes existing task and goal data without storing duplicates.
        Area area = new Area(UUID.randomUUID(), "Career", 10);
        LocalDate date = LocalDate.of(2026, 7, 1);
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Task dueToday = new Task(
                area,
                null,
                null,
                "Prepare weekly plan",
                null,
                TaskType.ACTION,
                TaskPriority.HIGH,
                Recurrence.NONE,
                Instant.parse("2026-07-01T15:00:00Z"),
                null,
                null,
                null
        );
        Task overdue = new Task(
                area,
                null,
                null,
                "Review backlog",
                null,
                TaskType.ACTION,
                TaskPriority.MEDIUM,
                Recurrence.NONE,
                Instant.parse("2026-06-30T15:00:00Z"),
                null,
                null,
                null
        );
        Task scheduled = new Task(
                area,
                null,
                null,
                "Focused work block",
                null,
                TaskType.ACTION,
                TaskPriority.MEDIUM,
                Recurrence.NONE,
                null,
                null,
                Instant.parse("2026-07-01T18:00:00Z"),
                Instant.parse("2026-07-01T19:00:00Z")
        );
        Goal activeGoal = new Goal(
                area,
                null,
                "Ship milestone",
                null,
                GoalType.CHECKLIST,
                Recurrence.NONE,
                null,
                null,
                null
        );

        when(taskRepository.findDashboardDueTasks(start, end, null)).thenReturn(List.of(dueToday));
        when(taskRepository.findDashboardOverdueTasks(start, null)).thenReturn(List.of(overdue));
        when(taskRepository.findDashboardScheduledTasks(start, end, null)).thenReturn(List.of(scheduled));
        when(goalRepository.findDashboardActiveGoals(null)).thenReturn(List.of(activeGoal));

        var result = dashboardService.getToday(date, null);

        assertThat(result.date()).isEqualTo(date);
        assertThat(result.tasksDueToday()).extracting("title").containsExactly("Prepare weekly plan");
        assertThat(result.overdueTasks()).extracting("title").containsExactly("Review backlog");
        assertThat(result.scheduledBlocks()).extracting("title").containsExactly("Focused work block");
        assertThat(result.activeGoals()).extracting("title").containsExactly("Ship milestone");
        assertThat(result.counts().dueToday()).isEqualTo(1);
        assertThat(result.counts().overdue()).isEqualTo(1);
        assertThat(result.counts().scheduled()).isEqualTo(1);
        assertThat(result.counts().activeGoals()).isEqualTo(1);
    }

    @Test
    void getTodayPassesAreaFilterToDashboardQueries() {
        // Area filtering belongs in the repository queries so the database can use indexed columns.
        UUID areaId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 1);
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        when(taskRepository.findDashboardDueTasks(start, end, areaId)).thenReturn(List.of());
        when(taskRepository.findDashboardOverdueTasks(start, areaId)).thenReturn(List.of());
        when(taskRepository.findDashboardScheduledTasks(start, end, areaId)).thenReturn(List.of());
        when(goalRepository.findDashboardActiveGoals(areaId)).thenReturn(List.of());

        dashboardService.getToday(date, areaId);

        verify(taskRepository).findDashboardDueTasks(start, end, areaId);
        verify(taskRepository).findDashboardOverdueTasks(start, areaId);
        verify(taskRepository).findDashboardScheduledTasks(start, end, areaId);
        verify(goalRepository).findDashboardActiveGoals(areaId);
    }
}
