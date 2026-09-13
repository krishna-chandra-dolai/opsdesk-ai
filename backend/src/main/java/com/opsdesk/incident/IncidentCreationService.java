package com.opsdesk.incident;

import com.opsdesk.ai.AiClassificationClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class IncidentCreationService {
    private final AiClassificationClient aiClient;
    private final IncidentService incidentService;

    public IncidentCreationService(AiClassificationClient aiClient, IncidentService incidentService) {
        this.aiClient = aiClient;
        this.incidentService = incidentService;
    }

    public IncidentResponse create(CreateIncidentRequest request, Authentication authentication) {
        var prediction = aiClient.classify(request.title(), request.description());
        return incidentService.create(request, authentication, prediction);
    }
}
