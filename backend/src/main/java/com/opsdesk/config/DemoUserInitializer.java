package com.opsdesk.config;

import com.opsdesk.user.Role;
import com.opsdesk.user.User;
import com.opsdesk.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "opsdesk.demo-users.enabled", havingValue = "true")
public class DemoUserInitializer implements ApplicationRunner {
    public static final String DEMO_PASSWORD = "DemoOnly123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("Demo Employee", "employee@opsdesk.demo", Role.EMPLOYEE);
        createIfMissing("Demo Engineer", "engineer@opsdesk.demo", Role.SUPPORT_ENGINEER);
        createIfMissing("Demo Admin", "admin@opsdesk.demo", Role.ADMIN);
    }

    private void createIfMissing(String name, String email, Role role) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            userRepository.save(new User(name, email, passwordEncoder.encode(DEMO_PASSWORD), role, Instant.now(clock)));
        }
    }
}
