package com.jat.app.dto.task;

import com.jat.app.entity.Recurrence;
import com.jat.app.entity.TaskPriority;
import com.jat.app.entity.TaskStatus;
import com.jat.app.entity.TaskType;

import java.time.Instant;
import java.util.UUID;

// Responses include related display labels so list screens do not need extra API calls per row.
public record TaskResponse(
        UUID id,
        UUID areaId,
        String areaName,
        UUID projectId,
        String projectName,
        UUID goalId,
        String goalTitle,
        String title,
        String description,
        TaskType taskType,
        TaskStatus status,
        TaskPriority priority,
        Recurrence recurrence,
        Instant dueAt,
        Instant remindAt,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
