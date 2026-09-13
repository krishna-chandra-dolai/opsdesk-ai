package com.opsdesk.incident;

import com.opsdesk.activity.IncidentActivityRepository;
import com.opsdesk.auth.LoginRequest;
import com.opsdesk.auth.LoginResponse;
import com.opsdesk.auth.RegisterRequest;
import com.opsdesk.comment.CommentRepository;
import com.opsdesk.comment.CommentRequest;
import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import com.opsdesk.user.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IncidentWorkflowIntegrationTest {
    private final TestRestTemplate http;
    private final IncidentActivityRepository activityRepository;
    private final CommentRepository commentRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    IncidentWorkflowIntegrationTest(TestRestTemplate http, IncidentActivityRepository activityRepository,
                                    CommentRepository commentRepository, IncidentRepository incidentRepository,
                                    UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.http = http;
        this.activityRepository = activityRepository;
        this.commentRepository = commentRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void clearDatabase() {
        commentRepository.deleteAll();
        activityRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void employeeAndEngineerCompleteLifecycleWithAuditTrail() {
        register("Employee", "employee@example.com");
        User engineer = saveUser("Engineer", "engineer@example.com", Role.SUPPORT_ENGINEER);
        String employeeToken = login("employee@example.com");
        String engineerToken = login("engineer@example.com");

        IncidentResponse incident = createIncident(employeeToken, "Cannot connect to VPN");

        var employeeAssign = patch("/api/incidents/" + incident.id() + "/assign",
                new AssignIncidentRequest(engineer.getId()), employeeToken, String.class);
        assertThat(employeeAssign.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        IncidentResponse assigned = patch("/api/incidents/" + incident.id() + "/assign",
                new AssignIncidentRequest(null), engineerToken, IncidentResponse.class).getBody();
        assertThat(assigned).isNotNull();
        assertThat(assigned.status()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(assigned.assignee().email()).isEqualTo("engineer@example.com");

        assertStatus(incident.id(), engineerToken, IncidentStatus.IN_PROGRESS);
        var comment = http.postForEntity("/api/incidents/" + incident.id() + "/comments",
                new HttpEntity<>(new CommentRequest("Investigating the VPN gateway logs."), bearer(engineerToken)),
                String.class);
        assertThat(comment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(comment.getBody()).contains("Investigating", "engineer@example.com");

        assertStatus(incident.id(), engineerToken, IncidentStatus.RESOLVED);
        assertStatus(incident.id(), employeeToken, IncidentStatus.REOPENED);
        IncidentResponse resumed = assertStatus(incident.id(), engineerToken, IncidentStatus.IN_PROGRESS);
        assertThat(resumed.assignee().email()).isEqualTo("engineer@example.com");
        assertStatus(incident.id(), engineerToken, IncidentStatus.RESOLVED);
        assertStatus(incident.id(), employeeToken, IncidentStatus.CLOSED);

        var invalid = patch("/api/incidents/" + incident.id() + "/status",
                new UpdateIncidentStatusRequest(IncidentStatus.IN_PROGRESS), engineerToken, String.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).contains("CLOSED -> IN_PROGRESS");

        var comments = http.exchange("/api/incidents/" + incident.id() + "/comments", HttpMethod.GET,
                new HttpEntity<>(bearer(employeeToken)), String.class);
        assertThat(comments.getBody()).contains("Investigating the VPN gateway logs");

        var activity = http.exchange("/api/incidents/" + incident.id() + "/activity", HttpMethod.GET,
                new HttpEntity<>(bearer(employeeToken)), String.class);
        assertThat(activity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(activity.getBody()).contains("INCIDENT_CREATED", "PRIORITY_CALCULATED", "ASSIGNED",
                "COMMENT_ADDED", "INCIDENT_RESOLVED", "INCIDENT_REOPENED", "INCIDENT_CLOSED");
    }

    @Test
    void adminAssignsOnlyToSupportEngineer() {
        register("Employee", "employee@example.com");
        User employee = userRepository.findByEmailIgnoreCase("employee@example.com").orElseThrow();
        User engineer = saveUser("Engineer", "engineer@example.com", Role.SUPPORT_ENGINEER);
        saveUser("Admin", "admin@example.com", Role.ADMIN);
        String employeeToken = login("employee@example.com");
        String adminToken = login("admin@example.com");
        IncidentResponse incident = createIncident(employeeToken, "Laptop will not start");

        var invalid = patch("/api/incidents/" + incident.id() + "/assign",
                new AssignIncidentRequest(employee.getId()), adminToken, String.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        IncidentResponse assigned = patch("/api/incidents/" + incident.id() + "/assign",
                new AssignIncidentRequest(engineer.getId()), adminToken, IncidentResponse.class).getBody();
        assertThat(assigned).isNotNull();
        assertThat(assigned.assignee().role()).isEqualTo(Role.SUPPORT_ENGINEER);

        var summary = http.exchange("/api/admin/summary", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), String.class);
        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summary.getBody()).contains("\"totalIncidents\":1", "\"openIncidents\":0");

        var employeeSummary = http.exchange("/api/admin/summary", HttpMethod.GET,
                new HttpEntity<>(bearer(employeeToken)), String.class);
        assertThat(employeeSummary.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void engineerCannotTakeAnotherEngineersAssignment() {
        register("Employee", "employee@example.com");
        saveUser("First engineer", "first@example.com", Role.SUPPORT_ENGINEER);
        saveUser("Second engineer", "second@example.com", Role.SUPPORT_ENGINEER);
        String firstToken = login("first@example.com");
        String secondToken = login("second@example.com");
        IncidentResponse incident = createIncident(login("employee@example.com"), "VPN disconnects");
        var assigned = patch("/api/incidents/" + incident.id() + "/assign",
                new AssignIncidentRequest(null), firstToken, IncidentResponse.class);
        assertThat(assigned.getStatusCode()).isEqualTo(HttpStatus.OK);
        long activityCount = activityRepository.count();

        var denied = patch("/api/incidents/" + incident.id() + "/assign",
                new AssignIncidentRequest(null), secondToken, String.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Incident stored = incidentRepository.findDetailedById(incident.id()).orElseThrow();
        assertThat(stored.getAssignee().getEmail()).isEqualTo("first@example.com");
        assertThat(activityRepository.count()).isEqualTo(activityCount);
    }

    private IncidentResponse assertStatus(Long id, String token, IncidentStatus status) {
        var response = patch("/api/incidents/" + id + "/status",
                new UpdateIncidentStatusRequest(status), token, IncidentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status);
        return response.getBody();
    }

    private IncidentResponse createIncident(String token, String title) {
        var response = http.postForEntity("/api/incidents",
                new HttpEntity<>(new CreateIncidentRequest(title, "Detailed diagnostic description",
                        Impact.HIGH, Urgency.MEDIUM), bearer(token)), IncidentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private User saveUser(String name, String email, Role role) {
        return userRepository.save(new User(name, email, passwordEncoder.encode("Password123!"), role, Instant.now()));
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

    private <T> org.springframework.http.ResponseEntity<T> patch(
            String path, Object body, String token, Class<T> responseType) {
        return http.exchange(path, HttpMethod.PATCH, new HttpEntity<>(body, bearer(token)), responseType);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
