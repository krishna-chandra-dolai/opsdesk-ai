package com.opsdesk.activity;

import com.opsdesk.incident.Incident;
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

import java.time.Instant;

@Entity
@Table(name = "incident_activities")
public class IncidentActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActivityAction action;

    @Column(name = "old_value", length = 254)
    private String oldValue;

    @Column(name = "new_value", length = 254)
    private String newValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IncidentActivity() {
    }

    public IncidentActivity(Incident incident, User actor, ActivityAction action,
                            String oldValue, String newValue, Instant createdAt) {
        this.incident = incident;
        this.actor = actor;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public User getActor() { return actor; }
    public ActivityAction getAction() { return action; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getCreatedAt() { return createdAt; }
}
