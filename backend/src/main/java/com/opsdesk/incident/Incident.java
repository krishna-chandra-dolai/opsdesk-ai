package com.opsdesk.incident;

import com.opsdesk.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_suggested_category", length = 20)
    private Category aiSuggestedCategory;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    protected Incident() {
    }

    public Incident(String title, String description, Impact impact, Urgency urgency,
                    Priority priority, User reporter, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.impact = impact;
        this.urgency = urgency;
        this.priority = priority;
        this.status = IncidentStatus.OPEN;
        this.category = Category.OTHER;
        this.reporter = reporter;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.slaBreached = false;
    }

    public Long getId() { return id; }
    public long getVersion() { return version; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Impact getImpact() { return impact; }
    public Urgency getUrgency() { return urgency; }
    public Priority getPriority() { return priority; }
    public IncidentStatus getStatus() { return status; }
    public Category getCategory() { return category; }
    public Category getAiSuggestedCategory() { return aiSuggestedCategory; }
    public Double getAiConfidence() { return aiConfidence; }
    public User getReporter() { return reporter; }
    public User getAssignee() { return assignee; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getSlaDeadline() { return slaDeadline; }
    public boolean isSlaBreached() { return slaBreached; }

    public void setSlaDeadline(Instant slaDeadline) {
        this.slaDeadline = slaDeadline;
    }

    public void markSlaBreached(Instant changedAt) {
        this.slaBreached = true;
        this.updatedAt = changedAt;
    }

    public void applyAiSuggestion(Category suggestedCategory, double confidence) {
        this.aiSuggestedCategory = suggestedCategory;
        this.aiConfidence = confidence;
    }

    public void assignTo(User user, Instant changedAt) {
        this.assignee = user;
        if (status == IncidentStatus.OPEN || status == IncidentStatus.REOPENED) {
            this.status = IncidentStatus.ASSIGNED;
        }
        this.updatedAt = changedAt;
    }

    public void changeStatus(IncidentStatus status, Instant changedAt) {
        this.status = status;
        this.updatedAt = changedAt;
        if (status == IncidentStatus.RESOLVED) {
            this.resolvedAt = changedAt;
        } else if (status == IncidentStatus.REOPENED) {
            this.resolvedAt = null;
            this.assignee = null;
        }
    }
}
