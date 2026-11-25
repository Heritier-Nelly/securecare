package com.securecare.service;

import com.securecare.domain.enums.UserRole;
import com.securecare.domain.model.UserDocument;
import com.securecare.repository.UserRepository;
import com.securecare.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);
    private static final String PASSWORD_RESET_URL = "https://securecare-pala.onrender.com/index.html?token=";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
    }

    public Map<String, Object> login(String username, String password, jakarta.servlet.http.HttpServletRequest request) {
        UserDocument user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        if (!user.active() || !encoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        UserDocument updated = new UserDocument(
                user.id(), user.username(), user.passwordHash(), user.role(), user.email(), true,
                user.createdAt(), Instant.now(), user.staffId(), user.patientId(),
                user.resetToken(), user.resetTokenExpiry());
        users.save(updated);
        audit.recordLogin(user, request);
        log.info("User authenticated username={} userId={}", user.username(), user.id());
        return Map.of(
                "token", jwt.generate(user.username(), user.role().name(), user.id()),
                "tokenType", "Bearer",
                "expiresInSeconds", jwt.expirationSeconds(),
                "user", Map.of(
                        "id", user.id(),
                        "username", user.username(),
                        "role", user.role().name(),
                        "email", user.email() == null ? "" : user.email()));
    }

    public Map<String, Object> register(String username, String password, String email, UserRole role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters");
        }
        if (users.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (role == null || role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Only doctor, nurse, receptionist, or patient roles can be requested");
        }
        UserDocument saved = users.save(new UserDocument(
                null, username, encoder.encode(password), role, email, false,
                Instant.now(), null, null, null, null, null));
        audit.record("REGISTER_USER", "users", saved.id(),
                Map.of("username", saved.username(), "role", saved.role().name(), "active", saved.active()), null);
        return Map.of(
                "id", saved.id(),
                "username", saved.username(),
                "role", saved.role(),
                "email", saved.email(),
                "active", saved.active(),
                "message", "Account created. Please wait for admin approval.");
    }

    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        users.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            Instant expiry = Instant.now().plusSeconds(15 * 60);
            UserDocument updated = new UserDocument(
                    user.id(), user.username(), user.passwordHash(), user.role(), user.email(),
                    user.active(), user.createdAt(), user.lastLoginAt(), user.staffId(), user.patientId(),
                    resetToken, expiry);
            users.save(updated);

            System.err.println("======================================================");
            System.err.println("PASSWORD RESET LINK FOR: " + user.email());
            System.err.println(PASSWORD_RESET_URL + resetToken);
            System.err.println("======================================================");
        });
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        if (newPassword == null || newPassword.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters");
        }

        UserDocument user = users.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));
        Instant expiry = user.resetTokenExpiry();
        if (expiry == null || !expiry.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        UserDocument updated = new UserDocument(
                user.id(), user.username(), encoder.encode(newPassword), user.role(), user.email(),
                user.active(), user.createdAt(), user.lastLoginAt(), user.staffId(), user.patientId(),
                null, null);
        users.save(updated);
    }
}
