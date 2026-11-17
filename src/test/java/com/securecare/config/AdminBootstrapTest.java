package com.securecare.config;

import com.securecare.domain.enums.UserRole;
import com.securecare.domain.model.UserDocument;
import com.securecare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {
    @Test
    void configuredExistingAccount_isPromotedWithoutReplacingPasswordOrLinks() throws Exception {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        UserDocument existing = new UserDocument(
                "user-1", "configured-admin", "existing-hash", UserRole.DOCTOR, "old@example.test",
                false, Instant.now(), null, "staff-1", null);
        when(users.findByUsername("configured-admin")).thenReturn(Optional.of(existing));
        when(users.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CommandLineRunner runner = new AdminBootstrap().createInitialAdmin(
                users, encoder, "configured-admin", "bootstrap-password-123", "admin@example.test");

        runner.run(new String[0]);

        var saved = org.mockito.ArgumentCaptor.forClass(UserDocument.class);
        verify(users).save(saved.capture());
        assertEquals(UserRole.ADMIN, saved.getValue().role());
        assertTrue(saved.getValue().active());
        assertEquals("existing-hash", saved.getValue().passwordHash());
        assertEquals("staff-1", saved.getValue().staffId());
        assertEquals("admin@example.test", saved.getValue().email());
        verifyNoInteractions(encoder);
    }

    @Test
    void missingConfiguredAccount_isCreatedWithEncodedPassword() throws Exception {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(users.findByUsername("configured-admin")).thenReturn(Optional.empty());
        when(encoder.encode("bootstrap-password-123")).thenReturn("encoded-hash");
        when(users.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CommandLineRunner runner = new AdminBootstrap().createInitialAdmin(
                users, encoder, "configured-admin", "bootstrap-password-123", "admin@example.test");

        runner.run(new String[0]);

        var saved = org.mockito.ArgumentCaptor.forClass(UserDocument.class);
        verify(users).save(saved.capture());
        assertEquals(UserRole.ADMIN, saved.getValue().role());
        assertTrue(saved.getValue().active());
        assertEquals("encoded-hash", saved.getValue().passwordHash());
    }
}
