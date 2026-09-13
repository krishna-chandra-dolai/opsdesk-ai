package com.opsdesk.incident;

import com.opsdesk.auth.LoginRequest;
import com.opsdesk.auth.LoginResponse;
import com.opsdesk.auth.RegisterRequest;
import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import com.opsdesk.user.UserRepository;
import com.opsdesk.comment.CommentRepository;
import com.opsdesk.activity.IncidentActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentIntegrationTest {
    private final TestRestTemplate http;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommentRepository commentRepository;
    private final IncidentActivityRepository activityRepository;

    @Autowired
    IncidentIntegrationTest(TestRestTemplate http, IncidentRepository incidentRepository,
                            UserRepository userRepository, PasswordEncoder passwordEncoder,
                            CommentRepository commentRepository,
                            IncidentActivityRepository activityRepository) {
        this.http = http;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
    }

    @BeforeEach
    void clearDatabase() {
        commentRepository.deleteAll();
        activityRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authenticatedEmployeeCreatesAndReadsPersistedIncident() {
        register("Reporter", "reporter@example.com");
        String reporterToken = login("reporter@example.com");

        var request = new CreateIncidentRequest("VPN disconnects", "VPN drops every five minutes",
                Impact.HIGH, Urgency.HIGH);
        var created = http.postForEntity("/api/incidents", new HttpEntity<>(request, bearer(reporterToken)),
                IncidentResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().priority()).isEqualTo(Priority.CRITICAL);
        assertThat(created.getBody().status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(created.getBody().reporter().email()).isEqualTo("reporter@example.com");
        assertThat(created.getBody().assignee()).isNull();
        assertThat(created.getBody().aiSuggestedCategory()).isNull();
        assertThat(created.getBody().slaDeadline()).isEqualTo(created.getBody().createdAt().plusSeconds(2 * 60 * 60));

        Incident persisted = incidentRepository.findDetailedById(created.getBody().id()).orElseThrow();
        assertThat(persisted.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(persisted.getReporter().getEmail()).isEqualTo("reporter@example.com");

        var detail = http.exchange("/api/incidents/" + created.getBody().id(), HttpMethod.GET,
                new HttpEntity<>(bearer(reporterToken)), IncidentResponse.class);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);

        var list = http.exchange("/api/incidents?size=10", HttpMethod.GET,
                new HttpEntity<>(bearer(reporterToken)), String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).contains("VPN disconnects", "\"totalElements\":1");
    }

    @Test
    void employeeCannotSeeAnotherEmployeesIncidentButEngineerCan() {
        register("First", "first@example.com");
        register("Second", "second@example.com");
        String firstToken = login("first@example.com");
        String secondToken = login("second@example.com");
        var request = new CreateIncidentRequest("Email issue", "Messages remain in the outbox",
                Impact.MEDIUM, Urgency.MEDIUM);
        IncidentResponse created = http.postForEntity("/api/incidents",
                new HttpEntity<>(request, bearer(firstToken)), IncidentResponse.class).getBody();
        assertThat(created).isNotNull();

        var hidden = http.exchange("/api/incidents/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(bearer(secondToken)), String.class);
        assertThat(hidden.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        userRepository.save(new User("Engineer", "engineer@example.com",
                passwordEncoder.encode("Password123!"), Role.SUPPORT_ENGINEER, Instant.now()));
        String engineerToken = login("engineer@example.com");
        var visible = http.exchange("/api/incidents/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(bearer(engineerToken)), IncidentResponse.class);
        assertThat(visible.getStatusCode()).isEqualTo(HttpStatus.OK);

        var engineerCreate = http.postForEntity("/api/incidents",
                new HttpEntity<>(request, bearer(engineerToken)), String.class);
        assertThat(engineerCreate.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(engineerCreate.getBody()).contains("\"timestamp\"", "\"status\":403",
                "\"message\":\"Access denied\"", "\"errors\":{}");
    }

    @Test
    void invalidValuesAndMissingResourcesUseReadableErrors() {
        register("Reporter", "reporter@example.com");
        String token = login("reporter@example.com");

        var invalid = http.postForEntity("/api/incidents",
                new HttpEntity<>(Map.of(
                        "title", "VPN issue",
                        "description", "VPN drops repeatedly",
                        "impact", "EXTREME",
                        "urgency", "HIGH"), bearer(token)), String.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).contains("\"status\":400", "Request contains an invalid value");

        var missingIncident = http.exchange("/api/incidents/999999", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertThat(missingIncident.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missingIncident.getBody()).contains("\"status\":404", "Incident not found");

        var missingRoute = http.exchange("/api/does-not-exist", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertThat(missingRoute.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missingRoute.getBody()).contains("\"status\":404", "API endpoint not found");
    }

    private void register(String name, String email) {
        var response = http.postForEntity("/api/auth/register",
                new RegisterRequest(name, email, "Password123!"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private String login(String email) {
        LoginResponse response = http.postForEntity("/api/auth/login",
                new LoginRequest(email, "Password123!"), LoginResponse.class).getBody();
        assertThat(response).isNotNull();
        return response.token();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
