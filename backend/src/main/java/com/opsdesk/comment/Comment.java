package com.opsdesk.comment;

import com.opsdesk.incident.Incident;
import com.opsdesk.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Comment() {
    }

    public Comment(Incident incident, User author, String content, Instant createdAt) {
        this.incident = incident;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
