package com.securecare.controller;

import com.securecare.domain.model.UserDocument;
import com.securecare.dto.UserDto;
import com.securecare.repository.UserRepository;
import com.securecare.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class UserProfileController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserProfileController(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    public record PasswordUpdateRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 12) String newPassword) {}
        public record ProfileUpdateRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank String currentPassword,
            String newPassword) {}

    @GetMapping({"/me", "/users/me"})
    public UserDto me(Authentication authentication) {
        return UserDto.from(currentUser(authentication));
    }

    public Map<String, Object> me() {
        return safe(currentUser());
    }

    @PatchMapping("/users/me")
    public Map<String, Object> updateProfile(@Valid @RequestBody ProfileUpdateRequest body, jakarta.servlet.http.HttpServletRequest request) {
        UserDocument user = currentUser();
        if (!encoder.matches(body.currentPassword(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        users.findByUsername(body.username()).filter(existing -> !existing.id().equals(user.id())).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        });
        if (body.newPassword() != null && !body.newPassword().isBlank() && body.newPassword().length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 12 characters");
        }

        String passwordHash = body.newPassword() == null || body.newPassword().isBlank()
                ? user.passwordHash()
                : encoder.encode(body.newPassword());
        UserDocument updated = users.save(new UserDocument(
            user.id(), body.username(), passwordHash, user.role(), body.email(), user.active(), user.createdAt(), user.lastLoginAt(), user.staffId(), user.patientId(),
            user.resetToken(), user.resetTokenExpiry()));
        audit.record("UPDATE_PROFILE", "users", user.id(), Map.of("username", updated.username(), "email", updated.email()), request);
        return safe(updated);
    }

    @PatchMapping("/users/me/password")
    public Map<String, Object> changePassword(@Valid @RequestBody PasswordUpdateRequest body, jakarta.servlet.http.HttpServletRequest request) {
        UserDocument user = currentUser();
        if (!encoder.matches(body.currentPassword(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        UserDocument updated = new UserDocument(
                user.id(),
                user.username(),
                encoder.encode(body.newPassword()),
                user.role(),
                user.email(),
                user.active(),
                user.createdAt(),
                user.lastLoginAt(),
                user.staffId(),
                user.patientId(),
                user.resetToken(),
                user.resetTokenExpiry()
        );
        users.save(updated);
        audit.record("UPDATE_PASSWORD","users",user.id(),Map.of("username",user.username()),request);
        return Map.of("success", true, "message", "Password updated successfully");
    }

    private UserDocument currentUser() {
        return currentUser(SecurityContextHolder.getContext().getAuthentication());
    }

    private UserDocument currentUser(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof String userId) || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be signed in");
        }
        return users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Map<String, Object> safe(UserDocument user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.id());
        result.put("username", user.username());
        result.put("role", user.role().name());
        result.put("email", user.email());
        result.put("active", user.active());
        result.put("staffId", user.staffId());
        result.put("patientId", user.patientId());
        result.put("createdAt", user.createdAt());
        result.put("lastLoginAt", user.lastLoginAt());
        return result;
    }
}
