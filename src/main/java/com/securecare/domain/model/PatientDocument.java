package com.securecare.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("patients")
public record PatientDocument(
        @Id String id,
        @Indexed(unique = true) String patientId,
        @TextIndexed String name,
        LocalDate dateOfBirth,
        String gender,
        String bloodType,
        List<String> allergies,
        List<MedicalHistoryEntry> medicalHistory,
        Contact contact,
        EmergencyContact emergencyContact,
        Instant createdAt,
        Instant updatedAt) {
    public record Contact(String phone, @TextIndexed String email, String address) { }
    public record EmergencyContact(String name, String relationship, String phone) { }
    public record MedicalHistoryEntry(String condition, String details, LocalDate recordedOn) { }
}
