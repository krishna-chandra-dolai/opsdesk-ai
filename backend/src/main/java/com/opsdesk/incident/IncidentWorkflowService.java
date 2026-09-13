package com.opsdesk.incident;

import com.opsdesk.activity.ActivityAction;
import com.opsdesk.activity.ActivityService;
import com.opsdesk.common.BadRequestException;
import com.opsdesk.common.ResourceNotFoundException;
import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import com.opsdesk.user.UserRepository;
import com.opsdesk.user.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class IncidentWorkflowService {
    private final IncidentAccess incidentAccess;
    private final UserService userService;
    private final UserRepository userRepository;
    private final IncidentTransitionValidator transitionValidator;
    private final ActivityService activityService;
    private final Clock clock;

    public IncidentWorkflowService(IncidentAccess incidentAccess, UserService userService,
                                   UserRepository userRepository, IncidentTransitionValidator transitionValidator,
                                   ActivityService activityService, Clock clock) {
        this.incidentAccess = incidentAccess;
        this.userService = userService;
        this.userRepository = userRepository;
        this.transitionValidator = transitionValidator;
        this.activityService = activityService;
        this.clock = clock;
    }

    @Transactional
    public IncidentResponse assign(Long incidentId, AssignIncidentRequest request, Authentication authentication) {
        User actor = userService.findByEmail(authentication.getName());
        Incident incident = incidentAccess.findVisible(incidentId, actor);
        if (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Resolved or closed incidents cannot be assigned");
        }
        User target = assignmentTarget(request, actor);
        if (actor.getRole() == Role.SUPPORT_ENGINEER && incident.getAssignee() != null
                && !incident.getAssignee().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only admins can reassign another engineer's incident");
        }
        String previousAssignee = incident.getAssignee() == null ? null : incident.getAssignee().getEmail();
        IncidentStatus previousStatus = incident.getStatus();
        incident.assignTo(target, Instant.now(clock));
        activityService.record(incident, actor, ActivityAction.ASSIGNED, previousAssignee, target.getEmail());
        if (previousStatus != incident.getStatus()) {
            activityService.record(incident, actor, ActivityAction.STATUS_CHANGED,
                    previousStatus.name(), incident.getStatus().name());
        }
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse updateStatus(Long incidentId, UpdateIncidentStatusRequest request,
                                         Authentication authentication) {
        User actor = userService.findByEmail(authentication.getName());
        Incident incident = incidentAccess.findVisible(incidentId, actor);
        IncidentStatus previous = incident.getStatus();
        IncidentStatus requested = request.status();
        transitionValidator.validate(previous, requested);
        authorizeTransition(incident, actor, requested);
        if (requested == IncidentStatus.ASSIGNED) {
            throw new BadRequestException("Use the assignment endpoint to move an incident to ASSIGNED");
        }
        if (previous == IncidentStatus.REOPENED && requested == IncidentStatus.IN_PROGRESS) {
            incident.assignTo(actor, Instant.now(clock));
        }
        incident.changeStatus(requested, Instant.now(clock));
        activityService.record(incident, actor, actionFor(requested), previous.name(), requested.name());
        return IncidentResponse.from(incident);
    }

    private User assignmentTarget(AssignIncidentRequest request, User actor) {
        if (actor.getRole() == Role.SUPPORT_ENGINEER) {
            if (request.assigneeId() != null && !request.assigneeId().equals(actor.getId())) {
                throw new AccessDeniedException("Support engineers can only claim incidents for themselves");
            }
            return actor;
        }
        if (actor.getRole() == Role.ADMIN) {
            if (request.assigneeId() == null) {
                throw new BadRequestException("assigneeId is required when an admin assigns an incident");
            }
            User target = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            if (target.getRole() != Role.SUPPORT_ENGINEER) {
                throw new BadRequestException("Incidents can only be assigned to support engineers");
            }
            return target;
        }
        throw new AccessDeniedException("Employees cannot assign incidents");
    }

    private void authorizeTransition(Incident incident, User actor, IncidentStatus requested) {
        if (actor.getRole() == Role.EMPLOYEE) {
            boolean ownsIncident = incident.getReporter().getId().equals(actor.getId());
            boolean employeeAction = requested == IncidentStatus.CLOSED || requested == IncidentStatus.REOPENED;
            if (!ownsIncident || !employeeAction) {
                throw new AccessDeniedException("Employees can only close or reopen their own resolved incidents");
            }
            return;
        }
        if (actor.getRole() != Role.SUPPORT_ENGINEER) {
            throw new AccessDeniedException("Only support engineers can perform this status change");
        }
        if (requested == IncidentStatus.CLOSED || requested == IncidentStatus.REOPENED) {
            throw new AccessDeniedException("Closing and reopening belong to the reporting employee");
        }
        if (incident.getAssignee() != null && !incident.getAssignee().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the assigned engineer can update this incident");
        }
    }

    private ActivityAction actionFor(IncidentStatus status) {
        return switch (status) {
            case RESOLVED -> ActivityAction.INCIDENT_RESOLVED;
            case CLOSED -> ActivityAction.INCIDENT_CLOSED;
            case REOPENED -> ActivityAction.INCIDENT_REOPENED;
            default -> ActivityAction.STATUS_CHANGED;
        };
    }
}
