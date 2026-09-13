package com.opsdesk.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull Impact impact,
        @NotNull Urgency urgency
) {
}
