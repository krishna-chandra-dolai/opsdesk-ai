package com.opsdesk.sla;

import com.opsdesk.incident.Priority;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SlaPolicyTest {
    private final SlaPolicy policy = new SlaPolicy();

    @Test
    void mapsPriorityToDocumentedTargets() {
        assertThat(policy.targetFor(Priority.CRITICAL)).isEqualTo(Duration.ofHours(2));
        assertThat(policy.targetFor(Priority.HIGH)).isEqualTo(Duration.ofHours(4));
        assertThat(policy.targetFor(Priority.MEDIUM)).isEqualTo(Duration.ofHours(8));
        assertThat(policy.targetFor(Priority.LOW)).isEqualTo(Duration.ofHours(24));
    }
}
