package com.jat.app.dto.goal;

import com.jat.app.entity.GoalStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGoalStatusRequest(
        // Goal status is updated separately from goal content to support lightweight workflow controls.
        @NotNull GoalStatus status
) {
}
