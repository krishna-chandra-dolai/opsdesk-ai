package com.opsdesk.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    long countByStatus(IncidentStatus status);
    long countBySlaBreachedTrue();
    long countByPriority(Priority priority);

    @Override
    @EntityGraph(attributePaths = {"reporter", "assignee"})
    Page<Incident> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "assignee"})
    Page<Incident> findByReporterEmailIgnoreCase(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "assignee"})
    Optional<Incident> findDetailedById(Long id);

    List<Incident> findBySlaBreachedFalseAndStatusNotInAndSlaDeadlineBefore(
            Collection<IncidentStatus> excludedStatuses, Instant now);
}
