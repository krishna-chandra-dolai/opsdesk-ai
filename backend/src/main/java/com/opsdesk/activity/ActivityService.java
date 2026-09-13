package com.opsdesk.activity;

import com.opsdesk.incident.Incident;
import com.opsdesk.incident.IncidentAccess;
import com.opsdesk.user.User;
import com.opsdesk.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ActivityService {
    private final IncidentActivityRepository activityRepository;
    private final IncidentAccess incidentAccess;
    private final UserService userService;
    private final Clock clock;

    public ActivityService(IncidentActivityRepository activityRepository, IncidentAccess incidentAccess,
                           UserService userService, Clock clock) {
        this.activityRepository = activityRepository;
        this.incidentAccess = incidentAccess;
        this.userService = userService;
        this.clock = clock;
    }

    public void record(Incident incident, User actor, ActivityAction action, String oldValue, String newValue) {
        activityRepository.save(new IncidentActivity(incident, actor, action, oldValue, newValue, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(Long incidentId, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        incidentAccess.findVisible(incidentId, user);
        return activityRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId)
                .stream().map(ActivityResponse::from).toList();
    }
}
