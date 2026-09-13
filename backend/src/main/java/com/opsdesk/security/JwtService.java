package com.opsdesk.security;

import com.opsdesk.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final long expirationSeconds;

    public JwtService(JwtEncoder jwtEncoder, Clock clock,
                      @Value("${opsdesk.jwt.expiration-seconds}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(User user) {
        Instant issuedAt = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("opsdesk-backend")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expirationSeconds))
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}
