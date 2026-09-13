package com.opsdesk.sla;

import com.opsdesk.activity.ActivityAction;
import com.opsdesk.activity.ActivityService;
import com.opsdesk.incident.Incident;
import com.opsdesk.incident.IncidentRepository;
import com.opsdesk.incident.IncidentStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Component
public class SlaBreachChecker {
    private static final Set<IncidentStatus> COMPLETED = Set.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);

    private final IncidentRepository incidentRepository;
    private final ActivityService activityService;
    private final Clock clock;

    public SlaBreachChecker(IncidentRepository incidentRepository, ActivityService activityService, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.activityService = activityService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${opsdesk.sla.check-interval-ms:60000}")
    @Transactional
    public void checkBreaches() {
        Instant now = Instant.now(clock);
        for (Incident incident : incidentRepository
                .findBySlaBreachedFalseAndStatusNotInAndSlaDeadlineBefore(COMPLETED, now)) {
            if (!incident.isSlaBreached()) {
                incident.markSlaBreached(now);
                activityService.record(incident, null, ActivityAction.SLA_BREACHED,
                        incident.getSlaDeadline().toString(), now.toString());
            }
        }
    }
}
