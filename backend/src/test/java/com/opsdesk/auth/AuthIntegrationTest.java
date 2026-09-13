package com.opsdesk.auth;

import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import com.opsdesk.user.UserRepository;
import com.opsdesk.incident.IncidentRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {
    private final TestRestTemplate http;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final IncidentRepository incidentRepository;
    private final CommentRepository commentRepository;
    private final IncidentActivityRepository activityRepository;

    @Autowired
    AuthIntegrationTest(TestRestTemplate http, UserRepository userRepository,
                        PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                        IncidentRepository incidentRepository, CommentRepository commentRepository,
                        IncidentActivityRepository activityRepository) {
        this.http = http;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.incidentRepository = incidentRepository;
        this.commentRepository = commentRepository;
        this.activityRepository = activityRepository;
    }

    @BeforeEach
    void clearUsers() {
        commentRepository.deleteAll();
        activityRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerLoginAndUseBearerToken() {
        var registration = http.postForEntity("/api/auth/register",
                new RegisterRequest("  Dev Candidate  ", "DEV@Example.com", "Password123!"), String.class);

        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registration.getBody())
                .contains("\"email\":\"dev@example.com\"")
                .contains("\"role\":\"EMPLOYEE\"")
                .doesNotContain("password", "Password123!");
        User stored = userRepository.findByEmailIgnoreCase("dev@example.com").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", stored.getPasswordHash())).isTrue();

        var login = http.postForEntity("/api/auth/login",
                new LoginRequest("dev@example.com", "Password123!"), LoginResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        assertThat(login.getBody().tokenType()).isEqualTo("Bearer");

        HttpHeaders headers = bearer(login.getBody().token());
        var me = http.exchange("/api/users/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).contains("dev@example.com").doesNotContain("passwordHash");

        var adminOnly = http.exchange("/api/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(adminOnly.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(adminOnly.getBody()).contains("\"status\":403");
    }

    @Test
    void rejectsDuplicateAndInvalidRegistration() {
        http.postForEntity("/api/auth/register",
                new RegisterRequest("First", "same@example.com", "Password123!"), String.class);

        var duplicate = http.postForEntity("/api/auth/register",
                new RegisterRequest("Second", "SAME@example.com", "Password123!"), String.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var invalid = http.postForEntity("/api/auth/register",
                Map.of("name", "", "email", "not-an-email", "password", "short"), String.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).contains("name", "email", "password");
    }

    @Test
    void authenticationFailuresReturn401() {
        var missing = http.getForEntity("/api/users/me", String.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missing.getBody()).contains("\"timestamp\"", "\"status\":401",
                "\"message\":\"Authentication required\"", "\"errors\":{}");

        var invalid = http.exchange("/api/users/me", HttpMethod.GET,
                new HttpEntity<>(bearer("not-a-jwt")), String.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var badLogin = http.postForEntity("/api/auth/login",
                new LoginRequest("missing@example.com", "wrong-password"), String.class);
        assertThat(badLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(badLogin.getBody()).contains("Invalid email or password");
    }

    @Test
    void expiredTokenReturns401() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("opsdesk-backend")
                .subject("expired@example.com")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .claim("role", Role.EMPLOYEE.name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        var response = http.exchange("/api/users/me", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
