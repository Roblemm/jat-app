package com.jat.app.dto.task;

import com.jat.app.entity.Recurrence;
import com.jat.app.entity.TaskPriority;
import com.jat.app.entity.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

// Task creation keeps area required and all narrower contexts optional for low-friction capture.
public record CreateTaskRequest(
        @NotNull UUID areaId,
        UUID projectId,
        UUID goalId,
        @NotBlank @Size(max = 180) String title,
        String description,
        @NotNull TaskType taskType,
        @NotNull TaskPriority priority,
        @NotNull Recurrence recurrence,
        Instant dueAt,
        Instant remindAt,
        Instant scheduledStart,
        Instant scheduledEnd
) {
}
