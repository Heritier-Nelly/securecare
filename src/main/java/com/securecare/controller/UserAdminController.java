package com.securecare.controller;

import com.securecare.domain.enums.UserRole;
import com.securecare.domain.model.UserDocument;
import com.securecare.dto.UserDto;
import com.securecare.repository.UserRepository;
import com.securecare.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping({"/api/users", "/api/admin/users"})
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private static final Set<String> PATCH_FIELDS = Set.of("email", "role", "active", "staffId");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserAdminController(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    public record CreateUser(
            @NotBlank String username,
            @NotBlank @Size(min = 12) String password,
            @NotNull UserRole role,
            @NotBlank @Email String email,
            String staffId,
            String patientId) {}

    public record UpdateUser(
            UserRole role,
            @Size(min = 12) String password,
            Boolean active,
            @Email String email,
            String staffId,
            String patientId) {}

    @GetMapping
    public List<Map<String, Object>> list() {
        return users.findAll().stream().map(this::safe).toList();
    }

    @GetMapping("/{id}")
    public UserDto getById(@PathVariable String id) {
        return UserDto.from(findUser(id));
    }

    @PostMapping
    public Map<String, Object> create(
            @Valid @RequestBody CreateUser body,
            HttpServletRequest request) {
        if (body.role() != UserRole.DOCTOR && body.role() != UserRole.NURSE && body.role() != UserRole.PATIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admins can create doctor, nurse, or patient accounts");
        }
        validateOwnershipLink(body.role(), body.staffId(), body.patientId());
        if (users.findByUsername(body.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserDocument user = users.save(new UserDocument(
                null,
                body.username(),
                encoder.encode(body.password()),
                body.role(),
                body.email(),
                true,
                Instant.now(),
                null,
                blankToNull(body.staffId()),
                blankToNull(body.patientId()),
                null,
                null));
        audit.record("CREATE_USER", "users", user.id(),
                Map.of("username", user.username(), "role", user.role().name()), request);
        return safe(user);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto patch(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        if (body.keySet().stream().anyMatch(field -> !PATCH_FIELDS.contains(field))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only email, role, and active may be updated");
        }

        UserDocument user = findUser(id);
        String email = user.email();
        UserRole role = user.role();
        boolean active = user.active();

        if (body.containsKey("email")) {
            Object value = body.get("email");
            if (!(value instanceof String text) || !text.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required");
            }
            email = text;
        }
        if (body.containsKey("role")) {
            Object value = body.get("role");
            if (!(value instanceof String text)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid role is required");
            }
            try {
                role = UserRole.valueOf(text);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid role is required");
            }
        }
        if (body.containsKey("active")) {
            Object value = body.get("active");
            if (!(value instanceof Boolean flag)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "active must be a boolean");
            }
            active = flag;
        }
        
        String staffId = user.staffId();
        if (body.containsKey("staffId")) {
            Object value = body.get("staffId");
            if (value != null && !(value instanceof String)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "staffId must be a string");
            }
            staffId = blankToNull((String) value);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean self = authentication != null && Objects.equals(authentication.getDetails(), id);
        if (self && (!active || role != user.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot deactivate yourself or change your own role");
        }
        ensureNotLastActiveAdmin(user, role, active);

        UserDocument saved = users.save(new UserDocument(
                user.id(), user.username(), user.passwordHash(), role, email, active,
            user.createdAt(), user.lastLoginAt(), staffId, user.patientId(),
            user.resetToken(), user.resetTokenExpiry()));
        audit.record("UPDATE_USER", "users", id,
                Map.of("role", saved.role().name(), "active", saved.active(), "staffId", saved.staffId() != null ? saved.staffId() : ""), request);
        return UserDto.from(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String id,
            HttpServletRequest request) {
        deactivateUser(id, request);
        return ResponseEntity.noContent().build();
    }

    public Map<String, Object> deactivate(String id, HttpServletRequest request) {
        return safe(deactivateUser(id, request));
    }

    @PatchMapping("/{id}/active")
    public Map<String, Object> active(
            @PathVariable String id,
            @Valid @RequestBody Map<String, Boolean> body,
            HttpServletRequest request) {
        if (!body.containsKey("active") || body.get("active") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "active is required");
        }

        UserDocument user = findUser(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Boolean.FALSE.equals(body.get("active"))
                && authentication != null && Objects.equals(authentication.getDetails(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }
        ensureNotLastActiveAdmin(user, user.role(), body.get("active"));

        UserDocument changed = new UserDocument(
                user.id(), user.username(), user.passwordHash(), user.role(), user.email(), body.get("active"),
            user.createdAt(), user.lastLoginAt(), user.staffId(), user.patientId(),
            user.resetToken(), user.resetTokenExpiry());
        UserDocument saved = users.save(changed);
        audit.record("UPDATE_USER", "users", id, Map.of("active", saved.active()), request);
        return safe(saved);
    }

    @PatchMapping("/{id}/ownership")
    public Map<String, Object> ownership(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        UserDocument user = users.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String staffId = null;
        String patientId = null;
        if (user.role() == UserRole.PATIENT) {
            patientId = requiredLinkField(body, "patientId");
        } else if (user.role() == UserRole.DOCTOR || user.role() == UserRole.NURSE) {
            staffId = requiredLinkField(body, "staffId");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This user role cannot be linked to a patient or staff record");
        }
        validateOwnershipLink(user.role(), staffId, patientId);

        UserDocument changed = new UserDocument(
                user.id(), user.username(), user.passwordHash(), user.role(), user.email(), user.active(),
            user.createdAt(), user.lastLoginAt(), blankToNull(staffId), blankToNull(patientId),
            user.resetToken(), user.resetTokenExpiry());
        UserDocument saved = users.save(changed);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("staffId", saved.staffId());
        details.put("patientId", saved.patientId());
        audit.record("UPDATE_USER_OWNERSHIP", "users", id, details, request);
        return safe(saved);
    }

    public Map<String, Object> update(String id, UpdateUser body, HttpServletRequest request) {
        UserDocument user = findUser(id);
        if (body.password() != null && body.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset password cannot be blank");
        }
        if (body.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admin roles cannot be assigned through user management");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean self = authentication != null && Objects.equals(authentication.getDetails(), id);
        if (self && (body.active() == Boolean.FALSE
                || (body.role() != null && body.role() != UserRole.ADMIN))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot deactivate yourself or change your own role");
        }

        UserRole role = body.role() == null ? user.role() : body.role();
        String staffId = body.staffId() == null ? user.staffId() : blankToNull(body.staffId());
        String patientId = body.patientId() == null ? user.patientId() : blankToNull(body.patientId());
        if (role != UserRole.DOCTOR && role != UserRole.NURSE) {
            staffId = null;
        }
        if (role != UserRole.PATIENT) {
            patientId = null;
        }
        validateOwnershipLink(role, staffId, patientId);
        UserDocument changed = new UserDocument(
                user.id(),
                user.username(),
                body.password() == null ? user.passwordHash() : encoder.encode(body.password()),
                role,
                body.email() == null ? user.email() : body.email(),
                body.active() == null ? user.active() : body.active(),
                user.createdAt(),
                user.lastLoginAt(),
                staffId,
                patientId,
                user.resetToken(),
                user.resetTokenExpiry());
        UserDocument saved = users.save(changed);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("role", saved.role().name());
        details.put("active", saved.active());
        details.put("passwordReset", body.password() != null);
        audit.record("UPDATE_USER", "users", id, details, request);
        return safe(saved);
    }

    private UserDocument deactivateUser(String id, HttpServletRequest request) {
        UserDocument user = findUser(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && Objects.equals(authentication.getDetails(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }
        if (!user.active()) {
            return user;
        }
        ensureNotLastActiveAdmin(user, user.role(), false);
        UserDocument saved = users.save(new UserDocument(
                user.id(), user.username(), user.passwordHash(), user.role(), user.email(), false,
            user.createdAt(), user.lastLoginAt(), user.staffId(), user.patientId(),
            user.resetToken(), user.resetTokenExpiry()));
        audit.record("DEACTIVATE_USER", "users", id, Map.of("active", false), request);
        return saved;
    }

    private void ensureNotLastActiveAdmin(UserDocument user, UserRole role, boolean active) {
        if (user.role() == UserRole.ADMIN && user.active()
                && (role != UserRole.ADMIN || !active)) {
            long activeAdmins = users.findAll().stream()
                    .filter(UserDocument::active)
                    .filter(candidate -> candidate.role() == UserRole.ADMIN)
                    .count();
            if (activeAdmins <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot deactivate or demote the last active administrator");
            }
        }
    }

    private String requiredLinkField(Map<String, String> body, String field) {
        if (!body.containsKey(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request must include " + field);
        }
        return body.get(field);
    }

    private UserDocument findUser(String id) {
        return users.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void validateOwnershipLink(UserRole role, String staffId, String patientId) {
        boolean hasStaffId = staffId != null && !staffId.isBlank();
        boolean hasPatientId = patientId != null && !patientId.isBlank();
        if (hasPatientId && role != UserRole.PATIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "patientId can only be assigned to a patient account");
        }
        if (hasStaffId && role != UserRole.DOCTOR && role != UserRole.NURSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "staffId can only be assigned to a doctor or nurse account");
        }
        if (hasStaffId && hasPatientId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A user cannot have both staffId and patientId");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
