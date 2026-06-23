package com.jat.app.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProjectRequest(
        @NotNull UUID areaId,
        @NotBlank @Size(max = 120) String name
) {
}
