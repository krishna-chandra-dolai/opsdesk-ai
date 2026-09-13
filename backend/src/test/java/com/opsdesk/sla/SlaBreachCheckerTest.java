package com.opsdesk.sla;

import com.opsdesk.activity.ActivityAction;
import com.opsdesk.activity.ActivityService;
import com.opsdesk.incident.Impact;
import com.opsdesk.incident.Incident;
import com.opsdesk.incident.IncidentRepository;
import com.opsdesk.incident.Priority;
import com.opsdesk.incident.Urgency;
import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlaBreachCheckerTest {
    @Test
    void marksOverdueIncidentAndRecordsBreachExactlyOnce() {
        Instant now = Instant.parse("2026-09-09T12:00:00Z");
        Instant createdAt = now.minusSeconds(25 * 60 * 60);
        User reporter = new User("Dev", "dev@example.com", "hash", Role.EMPLOYEE, createdAt);
        Incident incident = new Incident("Old issue", "Still unavailable", Impact.LOW, Urgency.LOW,
                Priority.LOW, reporter, createdAt);
        incident.setSlaDeadline(createdAt.plusSeconds(24 * 60 * 60));
        IncidentRepository repository = mock(IncidentRepository.class);
        ActivityService activityService = mock(ActivityService.class);
        when(repository.findBySlaBreachedFalseAndStatusNotInAndSlaDeadlineBefore(anySet(), any()))
                .thenReturn(List.of(incident));
        SlaBreachChecker checker = new SlaBreachChecker(repository, activityService,
                Clock.fixed(now, ZoneOffset.UTC));

        checker.checkBreaches();
        checker.checkBreaches();

        assertThat(incident.isSlaBreached()).isTrue();
        verify(activityService, times(1)).record(incident, null, ActivityAction.SLA_BREACHED,
                incident.getSlaDeadline().toString(), now.toString());
    }
}
