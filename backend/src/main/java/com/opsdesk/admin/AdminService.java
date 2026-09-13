package com.opsdesk.admin;

import com.opsdesk.incident.IncidentRepository;
import com.opsdesk.incident.IncidentStatus;
import com.opsdesk.incident.Priority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final IncidentRepository incidentRepository;

    public AdminService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        return new AdminSummaryResponse(
                incidentRepository.count(),
                incidentRepository.countByStatus(IncidentStatus.OPEN),
                incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS),
                incidentRepository.countByStatus(IncidentStatus.RESOLVED),
                incidentRepository.countBySlaBreachedTrue(),
                incidentRepository.countByPriority(Priority.CRITICAL));
    }
}
