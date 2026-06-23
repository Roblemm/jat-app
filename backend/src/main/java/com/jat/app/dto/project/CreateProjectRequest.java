package com.jat.app.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// Project creation is area-scoped because project names only need to be unique inside one area.
public record CreateProjectRequest(
        // The chosen area determines which project autocomplete list this record belongs to.
        @NotNull UUID areaId,
        // Store a concise display name; duplicate checks use Project.normalizedName.
        @NotBlank @Size(max = 120) String name
) {
}
