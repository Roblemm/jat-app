package com.jat.app.dto.task;

import com.jat.app.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        // Status updates are intentionally narrow so quick actions do not risk editing unrelated task fields.
        @NotNull TaskStatus status
) {
}
