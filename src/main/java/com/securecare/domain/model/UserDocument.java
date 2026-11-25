package com.securecare.domain.model;

import com.securecare.domain.enums.UserRole;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public record UserDocument(
        @Id String id,
        @Indexed(unique = true) String username,
        String passwordHash,
        UserRole role,
        String email,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt,
        String staffId,
        String patientId,
        String resetToken,
        Instant resetTokenExpiry) {
    public UserDocument(
            String id,
            String username,
            String passwordHash,
            UserRole role,
            String email,
            boolean active,
            Instant createdAt,
            Instant lastLoginAt,
            String staffId,
            String patientId) {
        this(id, username, passwordHash, role, email, active, createdAt, lastLoginAt,
                staffId, patientId, null, null);
    }

    public UserDocument(
            String id,
            String username,
            String passwordHash,
            UserRole role,
            String email,
            boolean active,
            Instant createdAt,
            Instant lastLoginAt) {
        this(id, username, passwordHash, role, email, active, createdAt, lastLoginAt,
                null, null, null, null);
    }
}
