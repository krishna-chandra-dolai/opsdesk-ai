package com.opsdesk.incident;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidentService;
    private final IncidentWorkflowService workflowService;
    private final IncidentCreationService creationService;

    public IncidentController(IncidentService incidentService, IncidentWorkflowService workflowService,
                              IncidentCreationService creationService) {
        this.incidentService = incidentService;
        this.workflowService = workflowService;
        this.creationService = creationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('EMPLOYEE')")
    public IncidentResponse create(@Valid @RequestBody CreateIncidentRequest request, Authentication authentication) {
        return creationService.create(request, authentication);
    }

    @GetMapping
    public Page<IncidentResponse> list(Pageable pageable, Authentication authentication) {
        return incidentService.list(authentication, pageable);
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable Long id, Authentication authentication) {
        return incidentService.get(id, authentication);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('SUPPORT_ENGINEER', 'ADMIN')")
    public IncidentResponse assign(@PathVariable Long id, @RequestBody AssignIncidentRequest request,
                                   Authentication authentication) {
        return workflowService.assign(id, request, authentication);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'SUPPORT_ENGINEER')")
    public IncidentResponse updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody UpdateIncidentStatusRequest request,
                                         Authentication authentication) {
        return workflowService.updateStatus(id, request, authentication);
    }
}
