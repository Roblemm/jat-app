package com.jat.app.dto.goal;

import com.jat.app.entity.GoalType;
import com.jat.app.entity.Recurrence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Goal creation is area-first, with optional project context and measurement-specific fields.
public record CreateGoalRequest(
        @NotNull UUID areaId,
        UUID projectId,
        @NotBlank @Size(max = 160) String title,
        String description,
        @NotNull GoalType goalType,
        @NotNull Recurrence recurrence,
        BigDecimal targetValue,
        @Size(max = 40) String unit,
        LocalDate targetDate
) {
}
