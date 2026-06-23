package com.jat.app.dto.area;

import java.util.UUID;

// Keep the area API contract small; clients only need identity, label, and display order.
public record AreaResponse(
        UUID id,
        String name,
        int displayOrder
) {
}
