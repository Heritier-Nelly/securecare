package com.securecare.controller;

import com.securecare.domain.enums.UserRole;
import com.securecare.domain.model.UserDocument;
import com.securecare.repository.UserRepository;
import com.securecare.service.AuditService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileControllerTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserProfileController controller = new UserProfileController(
            users, mock(PasswordEncoder.class), mock(AuditService.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void meFetchesAuthenticatedUserByIdAndDoesNotExposePasswordHash() {
        var authentication = new UsernamePasswordAuthenticationToken("not-the-id", null, List.of());
        authentication.setDetails("user-42");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDocument user = new UserDocument(
                "user-42", "patient", "sensitive-hash", UserRole.PATIENT, "patient@example.test",
                true, Instant.now(), null, null, "patient-42");
        when(users.findById("user-42")).thenReturn(Optional.of(user));

        Map<String, Object> profile = controller.me();

        verify(users).findById("user-42");
        verify(users, never()).findByUsername(anyString());
        assertEquals("user-42", profile.get("id"));
        assertEquals("patient-42", profile.get("patientId"));
        assertFalse(profile.containsKey("passwordHash"));
    }
}
