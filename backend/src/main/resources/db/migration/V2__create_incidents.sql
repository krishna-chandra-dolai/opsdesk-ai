CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    impact VARCHAR(10) NOT NULL,
    urgency VARCHAR(10) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    ai_suggested_category VARCHAR(20),
    ai_confidence DOUBLE PRECISION,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    assignee_id BIGINT REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    sla_deadline TIMESTAMPTZ,
    sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_incidents_impact CHECK (impact IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_incidents_urgency CHECK (urgency IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_incidents_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_incidents_status CHECK (status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED')),
    CONSTRAINT ck_incidents_category CHECK (category IN ('NETWORK', 'HARDWARE', 'SOFTWARE', 'ACCESS', 'EMAIL', 'OTHER')),
    CONSTRAINT ck_incidents_ai_category CHECK (ai_suggested_category IS NULL OR ai_suggested_category IN ('NETWORK', 'HARDWARE', 'SOFTWARE', 'ACCESS', 'EMAIL', 'OTHER')),
    CONSTRAINT ck_incidents_ai_confidence CHECK (ai_confidence IS NULL OR (ai_confidence >= 0 AND ai_confidence <= 1))
);

CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_priority ON incidents(priority);
CREATE INDEX idx_incidents_created_at ON incidents(created_at);
CREATE INDEX idx_incidents_assignee_id ON incidents(assignee_id);
CREATE INDEX idx_incidents_reporter_id ON incidents(reporter_id);
