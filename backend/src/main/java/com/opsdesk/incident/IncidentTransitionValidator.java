package com.opsdesk.incident;

import com.opsdesk.common.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class IncidentTransitionValidator {
    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED = Map.of(
            IncidentStatus.OPEN, Set.of(IncidentStatus.ASSIGNED),
            IncidentStatus.ASSIGNED, Set.of(IncidentStatus.IN_PROGRESS),
            IncidentStatus.IN_PROGRESS, Set.of(IncidentStatus.RESOLVED),
            IncidentStatus.RESOLVED, Set.of(IncidentStatus.CLOSED, IncidentStatus.REOPENED),
            IncidentStatus.REOPENED, Set.of(IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS),
            IncidentStatus.CLOSED, Set.of());

    public void validate(IncidentStatus current, IncidentStatus requested) {
        if (!ALLOWED.get(current).contains(requested)) {
            throw new BadRequestException("Invalid incident status transition: " + current + " -> " + requested);
        }
    }
}
