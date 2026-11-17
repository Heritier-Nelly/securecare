package com.securecare.service;

import com.securecare.domain.enums.UserRole;
import com.securecare.domain.model.UserDocument;
import com.securecare.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResourceServiceOwnershipTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ResourceAccessPolicy access = new ResourceAccessPolicy();
    private final ResourceService service = new ResourceService(
            mongo,
            users,
            mock(AuditService.class),
            mock(org.springframework.context.ApplicationEventPublisher.class),
            mock(AppointmentStateMachine.class),
            mock(ResourceValidator.class),
            mock(AppointmentFactory.class),
            new ResourceCollectionRegistry(),
            access,
            mock(ResourceSanitizer.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void patientList_filtersToLinkedPatientId() {
        authenticate("user-1", UserRole.PATIENT);
        when(users.findById("user-1")).thenReturn(Optional.of(user(UserRole.PATIENT, null, "patient-1")));
        when(mongo.find(any(Query.class), eq(org.bson.Document.class), eq("patients")))
                .thenReturn(List.of(new Document("patientId", "patient-1")));

        assertEquals(1, service.list("patients").size());

        var query = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(org.bson.Document.class), eq("patients"));
        assertEquals("patient-1", query.getValue().getQueryObject().getString("patientId"));
    }

    @Test
    void patient_canReadOwnRecord() {
        authenticate("user-1", UserRole.PATIENT);
        when(users.findById("user-1")).thenReturn(Optional.of(user(UserRole.PATIENT, null, "patient-1")));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("patients")))
                .thenReturn(new Document("_id", "record-1").append("patientId", "patient-1"));

        assertEquals("patient-1", service.get("patients", "record-1").get("patientId"));
    }

    @Test
    void patient_cannotReadOtherPatientsRecord() {
        authenticate("user-1", UserRole.PATIENT);
        when(users.findById("user-1")).thenReturn(Optional.of(user(UserRole.PATIENT, null, "patient-1")));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("patients")))
                .thenReturn(null)
                .thenReturn(new Document("_id", "record-2").append("patientId", "patient-2"));

        assertThrows(AccessDeniedException.class, () -> service.get("patients", "record-2"));
    }

    @Test
    void doctor_canReadOwnPatientsRecord() {
        authenticate("user-2", UserRole.DOCTOR);
        when(users.findById("user-2")).thenReturn(Optional.of(user(UserRole.DOCTOR, "doctor-7", null)));
        when(mongo.find(any(Query.class), eq(Document.class), eq("appointments")))
                .thenReturn(List.of(new Document("patientId", "patient-1")));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("patients")))
                .thenReturn(new Document("_id", "record-1").append("patientId", "patient-1"));

        assertEquals("patient-1", service.get("patients", "record-1").get("patientId"));
    }

    @Test
    void doctor_cannotReadOtherDoctorsPatient() {
        authenticate("user-2", UserRole.DOCTOR);
        when(users.findById("user-2")).thenReturn(Optional.of(user(UserRole.DOCTOR, "doctor-7", null)));
        when(mongo.find(any(Query.class), eq(Document.class), eq("appointments")))
                .thenReturn(List.of(new Document("patientId", "patient-1")));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("patients")))
                .thenReturn(null)
                .thenReturn(new Document("_id", "record-2").append("patientId", "patient-2"));

        assertThrows(AccessDeniedException.class, () -> service.get("patients", "record-2"));
    }

    @Test
    void admin_canReadAnyRecord() {
        authenticate("admin-1", UserRole.ADMIN);
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("patients")))
                .thenReturn(new Document("_id", "record-2").append("patientId", "patient-2"));

        assertEquals("patient-2", service.get("patients", "record-2").get("patientId"));
        verifyNoInteractions(users);
    }

    @Test
    void receptionist_cannotReadMedicalRecords() {
        authenticate("receptionist-1", UserRole.RECEPTIONIST);

        assertThrows(ResponseStatusException.class, () -> service.get("medical-records", "record-1"));
        verifyNoInteractions(users, mongo);
    }

    @Test
    void doctorList_filtersToPatientsWithDoctorAppointments() {
        authenticate("user-2", UserRole.DOCTOR);
        when(users.findById("user-2")).thenReturn(Optional.of(user(UserRole.DOCTOR, "doctor-7", null)));
        when(mongo.find(any(Query.class), eq(org.bson.Document.class), eq("appointments")))
                .thenReturn(List.of(new Document("patientId", "patient-1"), new Document("patientId", "patient-2")));
        when(mongo.find(any(Query.class), eq(org.bson.Document.class), eq("patients"))).thenReturn(List.of());

        assertTrue(service.list("patients").isEmpty());

        var assignmentQuery = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(assignmentQuery.capture(), eq(org.bson.Document.class), eq("appointments"));
        assertEquals("doctor-7", assignmentQuery.getValue().getQueryObject().getString("doctorId"));

        var query = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(org.bson.Document.class), eq("patients"));
        Document queryDocument = query.getValue().getQueryObject();
        String queryJson = queryDocument.toJson();
        assertTrue(queryJson.contains("patient-1"));
        assertTrue(queryJson.contains("patient-2"));
    }

    @Test
    void doctorClinicalRecordList_requiresBothAssignmentAndDoctorOwnership() {
        authenticate("user-2", UserRole.DOCTOR);
        when(users.findById("user-2")).thenReturn(Optional.of(user(UserRole.DOCTOR, "doctor-7", null)));
        when(mongo.find(any(Query.class), eq(org.bson.Document.class), eq("appointments")))
                .thenReturn(List.of(new Document("patientId", "patient-1")));
        when(mongo.find(any(Query.class), eq(org.bson.Document.class), eq("medical_records"))).thenReturn(List.of());

        assertTrue(service.list("medical-records").isEmpty());

        var query = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(org.bson.Document.class), eq("medical_records"));
        String queryJson = query.getValue().getQueryObject().toJson();
        assertTrue(queryJson.contains("doctor-7"));
        assertTrue(queryJson.contains("patient-1"));
    }

    @Test
    void doctorCannotCreateRecordForUnassignedPatient() {
        authenticate("user-2", UserRole.DOCTOR);
        when(users.findById("user-2")).thenReturn(Optional.of(user(UserRole.DOCTOR, "doctor-7", null)));
        when(mongo.find(any(Query.class), eq(org.bson.Document.class), eq("appointments")))
                .thenReturn(List.of(new Document("patientId", "patient-1")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create("medical-records", Map.of("patientId", "patient-2"), mock(HttpServletRequest.class)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(mongo, never()).insert(any(), anyString());
    }

    @Test
    void patientCannotCreateResource() {
        authenticate("user-1", UserRole.PATIENT);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create("patients", Map.of(), mock(HttpServletRequest.class)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(users, mongo);
    }

    private void authenticate(String userId, UserRole role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                role.name().toLowerCase(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private UserDocument user(UserRole role, String staffId, String patientId) {
        return new UserDocument("unused", "username", "hash", role, "user@example.test",
                true, Instant.now(), null, staffId, patientId);
    }
}
