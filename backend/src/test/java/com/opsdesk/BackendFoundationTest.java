package com.opsdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendFoundationTest {
    private final TestRestTemplate http;
    private final JdbcTemplate jdbc;

    @Autowired
    BackendFoundationTest(TestRestTemplate http, JdbcTemplate jdbc) {
        this.http = http;
        this.jdbc = jdbc;
    }

    @Test
    void healthReportsUpWithPostgresConnected() {
        var response = http.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .isEqualTo("opsdesk_test");
    }

    @Test
    void sensitiveActuatorEndpointsAreNotExposed() {
        var response = http.getForEntity("/actuator/env", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).doesNotContain("trace", "password");
    }
}
