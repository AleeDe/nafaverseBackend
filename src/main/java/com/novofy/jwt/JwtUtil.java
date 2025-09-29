package com.novofy.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret:mysecretkeymysecretkeymysecretkey!as}")
    private String secret;

    @Value("${app.jwt.expiration-ms:600000}") // 10 minutes default
    private long expirationMs;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Generate JWT token
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        Claims c = parseClaims(token);
        return c != null ? c.getSubject() : null;
    }

    public String extractRole(String token) {
        Claims c = parseClaims(token);
        return c != null ? (String) c.get("role") : null;
    }

    public boolean isValid(String token) {
        Claims c = parseClaims(token);
        return c != null && c.getExpiration() != null && c.getExpiration().after(new Date());
    }

    public boolean validateToken(String token, String email) {
        Claims c = parseClaims(token);
        return c != null
                && email != null
                && email.equals(c.getSubject())
                && c.getExpiration() != null
                && c.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            // invalid signature, expired, malformed, unsupported, or empty
            return null;
        }
    }
}
