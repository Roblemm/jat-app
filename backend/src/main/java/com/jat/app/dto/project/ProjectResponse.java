package com.jat.app.dto.project;

import java.time.Instant;
import java.util.UUID;

// Project responses include area context so clients can group or filter without an extra lookup.
public record ProjectResponse(
        UUID id,
        UUID areaId,
        String areaName,
        // name is the display value; normalizedName is exposed for predictable client-side comparisons.
        String name,
        String normalizedName,
        Instant createdAt,
        Instant updatedAt
) {
}
