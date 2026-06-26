package com.jat.app.repository;

import com.jat.app.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    // Area-scoped lists power the default task inbox and prevent unrelated contexts from mixing.
    List<Task> findAllByAreaIdOrderByCreatedAtDesc(UUID areaId);

    // Project filtering supports focused task lists without making projects mandatory.
    List<Task> findAllByAreaIdAndProjectIdOrderByCreatedAtDesc(UUID areaId, UUID projectId);

    // Goal filtering supports progress views where tasks become the checklist for a goal.
    List<Task> findAllByAreaIdAndGoalIdOrderByCreatedAtDesc(UUID areaId, UUID goalId);

    // When both filters are present, keep the query explicit instead of relying on in-memory filtering.
    List<Task> findAllByAreaIdAndProjectIdAndGoalIdOrderByCreatedAtDesc(UUID areaId, UUID projectId, UUID goalId);

    // Due-today tasks are deadline-based and exclude completed/archived work from the active Today view.
    @Query("""
            select t from Task t
            where t.dueAt >= :start
              and t.dueAt < :end
              and t.status <> com.jat.app.entity.TaskStatus.COMPLETED
              and t.status <> com.jat.app.entity.TaskStatus.ARCHIVED
              and (:areaId is null or t.area.id = :areaId)
            order by t.dueAt asc
            """)
    List<Task> findDashboardDueTasks(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("areaId") UUID areaId
    );

    // Overdue tasks are anything still active with a deadline before the selected day begins.
    @Query("""
            select t from Task t
            where t.dueAt < :start
              and t.status <> com.jat.app.entity.TaskStatus.COMPLETED
              and t.status <> com.jat.app.entity.TaskStatus.ARCHIVED
              and (:areaId is null or t.area.id = :areaId)
            order by t.dueAt asc
            """)
    List<Task> findDashboardOverdueTasks(
            @Param("start") Instant start,
            @Param("areaId") UUID areaId
    );

    // Scheduled blocks are calendar-based; they can exist with or without a task deadline.
    @Query("""
            select t from Task t
            where t.scheduledStart >= :start
              and t.scheduledStart < :end
              and t.status <> com.jat.app.entity.TaskStatus.COMPLETED
              and t.status <> com.jat.app.entity.TaskStatus.ARCHIVED
              and (:areaId is null or t.area.id = :areaId)
            order by t.scheduledStart asc
            """)
    List<Task> findDashboardScheduledTasks(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("areaId") UUID areaId
    );
}
