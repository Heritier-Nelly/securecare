package com.securecare.controller;

import com.securecare.domain.enums.UserRole;
import com.securecare.domain.model.UserDocument;
import com.securecare.repository.UserRepository;
import com.securecare.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserAdminControllerOwnershipTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserAdminController controller = new UserAdminController(
            users, mock(PasswordEncoder.class), mock(AuditService.class));

    @Test
    void ownershipLink_updatesPatientIdAndPreservesAccountFields() {
        UserDocument original = new UserDocument(
                "user-1", "patient", "hash", UserRole.PATIENT, "patient@example.test",
                true, Instant.now(), null);
        when(users.findById("user-1")).thenReturn(Optional.of(original));
        when(users.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = controller.ownership(
                "user-1", Map.of("patientId", "patient-17"), mock(HttpServletRequest.class));

        var saved = org.mockito.ArgumentCaptor.forClass(UserDocument.class);
        verify(users).save(saved.capture());
        assertEquals("patient-17", saved.getValue().patientId());
        assertNull(saved.getValue().staffId());
        assertEquals("patient-17", response.get("patientId"));
        assertEquals("hash", saved.getValue().passwordHash());
        assertEquals(original.createdAt(), saved.getValue().createdAt());
    }

    @Test
    void ownershipLink_rejectsLinkFieldForWrongRole() {
        UserDocument doctor = new UserDocument(
                "user-2", "doctor", "hash", UserRole.DOCTOR, "doctor@example.test",
                true, Instant.now(), null);
        when(users.findById("user-2")).thenReturn(Optional.of(doctor));

        assertThrows(ResponseStatusException.class,
                () -> controller.ownership("user-2", Map.of("patientId", "patient-17"), mock(HttpServletRequest.class)));

        verify(users, never()).save(any(UserDocument.class));
    }

    @Test
    void update_changesRoleAndResetsPasswordWithoutLosingOwnership() {
        UserDocument doctor = new UserDocument(
                "user-3", "doctor", "old-hash", UserRole.DOCTOR, "doctor@example.test",
                true, Instant.now(), null, "staff-3", null);
        when(users.findById("user-3")).thenReturn(Optional.of(doctor));
        when(users.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("temporary-password")).thenReturn("new-hash");
        UserAdminController subject = new UserAdminController(users, encoder, mock(AuditService.class));

        subject.update("user-3", new UserAdminController.UpdateUser(
                UserRole.NURSE, "temporary-password", false, null, null, null), mock(HttpServletRequest.class));

        var saved = org.mockito.ArgumentCaptor.forClass(UserDocument.class);
        verify(users).save(saved.capture());
        assertEquals(UserRole.NURSE, saved.getValue().role());
        assertEquals("new-hash", saved.getValue().passwordHash());
        assertFalse(saved.getValue().active());
        assertEquals("staff-3", saved.getValue().staffId());
    }

    @Test
    void delete_softDeactivatesAccountAndPreservesCredentials() {
        UserDocument patient = new UserDocument(
                "user-4", "patient", "hash", UserRole.PATIENT, "patient@example.test",
                true, Instant.now(), null, null, "patient-4");
        when(users.findById("user-4")).thenReturn(Optional.of(patient));
        when(users.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = controller.deactivate("user-4", mock(HttpServletRequest.class));

        var saved = org.mockito.ArgumentCaptor.forClass(UserDocument.class);
        verify(users).save(saved.capture());
        assertFalse(saved.getValue().active());
        assertEquals("hash", saved.getValue().passwordHash());
        assertEquals("patient-4", saved.getValue().patientId());
        assertEquals(false, response.get("active"));
    }
}
