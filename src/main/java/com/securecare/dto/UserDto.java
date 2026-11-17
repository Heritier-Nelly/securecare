package com.securecare.dto;

import com.securecare.domain.model.UserDocument;
import java.time.Instant;

public record UserDto(
        String id,
        String username,
        String role,
        String email,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt) {

    public static UserDto from(UserDocument user) {
        return new UserDto(
                user.id(),
                user.username(),
                user.role().name(),
                user.email(),
                user.active(),
                user.createdAt(),
                user.lastLoginAt());
    }
}
