package com.securecare.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:900000}") long expirationMs) {
        if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET must be configured");
        byte[] bytes;
        try { bytes = java.util.Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException ex) { bytes = secret.getBytes(StandardCharsets.UTF_8); }
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes (use Base64)");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generate(String username, String role, String userId) {
        Date now = new Date();
        return Jwts.builder().subject(username).claim("role", role).claim("uid", userId)
                .issuedAt(now).expiration(new Date(now.getTime() + expirationMs)).signWith(key).compact();
    }
    public long expirationSeconds() { return expirationMs / 1000; }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
    public boolean isValid(String token, String username) {
        try { Claims claims = parse(token); return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date()); }
        catch (RuntimeException ex) { return false; }
    }
}
