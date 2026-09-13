package com.opsdesk.incident;

import com.opsdesk.activity.ActivityAction;
import com.opsdesk.activity.ActivityService;
import com.opsdesk.common.ResourceNotFoundException;
import com.opsdesk.user.User;
import com.opsdesk.user.UserRepository;
import com.opsdesk.sla.SlaPolicy;
import com.opsdesk.ai.AiPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final PriorityCalculator priorityCalculator;
    private final IncidentAccess incidentAccess;
    private final ActivityService activityService;
    private final SlaPolicy slaPolicy;
    private final Clock clock;

    public IncidentService(IncidentRepository incidentRepository, UserRepository userRepository,
                           PriorityCalculator priorityCalculator, IncidentAccess incidentAccess,
                           ActivityService activityService, SlaPolicy slaPolicy, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.priorityCalculator = priorityCalculator;
        this.incidentAccess = incidentAccess;
        this.activityService = activityService;
        this.slaPolicy = slaPolicy;
        this.clock = clock;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request, Authentication authentication,
                                   Optional<AiPrediction> prediction) {
        User reporter = currentUser(authentication);
        Priority priority = priorityCalculator.calculate(request.impact(), request.urgency());
        Instant createdAt = Instant.now(clock);
        Incident incident = new Incident(request.title().trim(), request.description().trim(), request.impact(),
                request.urgency(), priority, reporter, createdAt);
        incident.setSlaDeadline(slaPolicy.deadline(createdAt, priority));
        prediction.ifPresent(result -> incident.applyAiSuggestion(result.category(), result.confidence()));
        Incident saved = incidentRepository.save(incident);
        activityService.record(saved, reporter, ActivityAction.INCIDENT_CREATED, null, IncidentStatus.OPEN.name());
        activityService.record(saved, reporter, ActivityAction.PRIORITY_CALCULATED, null, priority.name());
        prediction.ifPresent(result -> activityService.record(saved, reporter,
                ActivityAction.AI_CATEGORY_SUGGESTED, null, result.category().name()));
        return IncidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> list(Authentication authentication, Pageable pageable) {
        User user = currentUser(authentication);
        return incidentAccess.visibleTo(user, pageable).map(IncidentResponse::from);
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(Long id, Authentication authentication) {
        User user = currentUser(authentication);
        return IncidentResponse.from(incidentAccess.findVisible(id, user));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
