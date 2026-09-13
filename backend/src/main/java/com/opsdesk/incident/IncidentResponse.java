package com.opsdesk.incident;

import com.opsdesk.user.UserResponse;

import java.time.Instant;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        Impact impact,
        Urgency urgency,
        Priority priority,
        IncidentStatus status,
        Category category,
        Category aiSuggestedCategory,
        Double aiConfidence,
        UserResponse reporter,
        UserResponse assignee,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        Instant slaDeadline,
        boolean slaBreached
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(), incident.getTitle(), incident.getDescription(), incident.getImpact(),
                incident.getUrgency(), incident.getPriority(), incident.getStatus(), incident.getCategory(),
                incident.getAiSuggestedCategory(), incident.getAiConfidence(), UserResponse.from(incident.getReporter()),
                incident.getAssignee() == null ? null : UserResponse.from(incident.getAssignee()),
                incident.getCreatedAt(), incident.getUpdatedAt(), incident.getResolvedAt(),
                incident.getSlaDeadline(), incident.isSlaBreached());
    }
}
