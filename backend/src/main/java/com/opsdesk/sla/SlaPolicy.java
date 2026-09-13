package com.opsdesk.sla;

import com.opsdesk.incident.Priority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class SlaPolicy {
    public Duration targetFor(Priority priority) {
        return switch (priority) {
            case CRITICAL -> Duration.ofHours(2);
            case HIGH -> Duration.ofHours(4);
            case MEDIUM -> Duration.ofHours(8);
            case LOW -> Duration.ofHours(24);
        };
    }

    public Instant deadline(Instant createdAt, Priority priority) {
        return createdAt.plus(targetFor(priority));
    }
}
