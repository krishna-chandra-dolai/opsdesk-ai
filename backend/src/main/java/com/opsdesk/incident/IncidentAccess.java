package com.opsdesk.incident;

import com.opsdesk.common.ResourceNotFoundException;
import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class IncidentAccess {
    private final IncidentRepository incidentRepository;

    public IncidentAccess(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public Page<Incident> visibleTo(User user, Pageable pageable) {
        if (user.getRole() == Role.EMPLOYEE) {
            return incidentRepository.findByReporterEmailIgnoreCase(user.getEmail(), pageable);
        }
        return incidentRepository.findAll(pageable);
    }

    public Incident findVisible(Long id, User user) {
        Incident incident = incidentRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
        if (user.getRole() == Role.EMPLOYEE && !incident.getReporter().getId().equals(user.getId())) {
            // Hiding existence avoids leaking another employee's ticket identifiers.
            throw new ResourceNotFoundException("Incident not found");
        }
        return incident;
    }
}
