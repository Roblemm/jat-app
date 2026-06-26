package com.jat.app.dto.error;

// Minimal error contract for client-correctable request problems.
public record ErrorResponse(String message) {
}
