package com.opsdesk.incident;

import jakarta.validation.constraints.NotNull;

public record UpdateIncidentStatusRequest(@NotNull IncidentStatus status) {
}
