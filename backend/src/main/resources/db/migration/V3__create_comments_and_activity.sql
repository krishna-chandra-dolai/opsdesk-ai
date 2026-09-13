CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id),
    author_id BIGINT NOT NULL REFERENCES users(id),
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_comments_incident_id ON comments(incident_id);

CREATE TABLE incident_activities (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL REFERENCES incidents(id),
    actor_id BIGINT REFERENCES users(id),
    action VARCHAR(40) NOT NULL,
    old_value VARCHAR(254),
    new_value VARCHAR(254),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_incident_activities_action CHECK (action IN (
        'INCIDENT_CREATED', 'ASSIGNED', 'STATUS_CHANGED', 'PRIORITY_CALCULATED',
        'AI_CATEGORY_SUGGESTED', 'COMMENT_ADDED', 'SLA_BREACHED',
        'INCIDENT_RESOLVED', 'INCIDENT_CLOSED', 'INCIDENT_REOPENED'
    ))
);

CREATE INDEX idx_incident_activities_incident_id ON incident_activities(incident_id);
