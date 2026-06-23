package com.jat.app.dto.area;

import java.util.UUID;

public record AreaResponse(
        UUID id,
        String name,
        int displayOrder
) {
}
