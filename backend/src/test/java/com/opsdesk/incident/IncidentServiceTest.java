package com.opsdesk.incident;

import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import com.opsdesk.user.UserRepository;
import com.opsdesk.activity.ActivityService;
import com.opsdesk.sla.SlaPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private IncidentAccess incidentAccess;
    @Mock
    private ActivityService activityService;

    @Test
    void createCalculatesPriorityAndStoresSafeDefaults() {
        Instant now = Instant.parse("2026-09-09T10:00:00Z");
        User reporter = new User("Dev", "dev@example.com", "stored-hash", Role.EMPLOYEE, now);
        when(userRepository.findByEmailIgnoreCase("dev@example.com")).thenReturn(Optional.of(reporter));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        IncidentService service = new IncidentService(incidentRepository, userRepository,
                new PriorityCalculator(), incidentAccess, activityService, new SlaPolicy(),
                Clock.fixed(now, ZoneOffset.UTC));

        var authentication = new UsernamePasswordAuthenticationToken("dev@example.com", "", List.of());
        IncidentResponse response = service.create(
                new CreateIncidentRequest(" VPN unavailable ", " Cannot reach internal systems ",
                        Impact.HIGH, Urgency.HIGH), authentication, Optional.empty());

        ArgumentCaptor<Incident> saved = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(saved.capture());
        verify(activityService).record(saved.getValue(), reporter,
                com.opsdesk.activity.ActivityAction.INCIDENT_CREATED, null, IncidentStatus.OPEN.name());
        assertThat(saved.getValue().getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(response.title()).isEqualTo("VPN unavailable");
        assertThat(response.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(response.category()).isEqualTo(Category.OTHER);
        assertThat(response.assignee()).isNull();
        assertThat(response.aiSuggestedCategory()).isNull();
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.slaDeadline()).isEqualTo(now.plusSeconds(2 * 60 * 60));
    }
}
