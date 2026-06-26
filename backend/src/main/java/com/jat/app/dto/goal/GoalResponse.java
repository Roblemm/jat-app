package com.jat.app.dto.goal;

import com.jat.app.entity.GoalStatus;
import com.jat.app.entity.GoalType;
import com.jat.app.entity.Recurrence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Goal responses include area/project labels so list views can render without extra lookups.
public record GoalResponse(
        UUID id,
        UUID areaId,
        String areaName,
        UUID projectId,
        String projectName,
        String title,
        String description,
        GoalType goalType,
        GoalStatus status,
        Recurrence recurrence,
        BigDecimal targetValue,
        String unit,
        LocalDate targetDate,
        Instant createdAt,
        Instant updatedAt
) {
}
