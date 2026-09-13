package com.opsdesk.admin;

public record AdminSummaryResponse(long totalIncidents, long openIncidents, long inProgressIncidents,
                                   long resolvedIncidents, long slaBreachedIncidents, long criticalIncidents) {
}
