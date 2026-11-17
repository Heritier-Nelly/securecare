package com.securecare.domain.model;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("medical_records")
@CompoundIndex(name = "patient_visit_date_idx", def = "{'patientId': 1, 'visitDate': -1}")
public record MedicalRecordDocument(
        @Id String id,
        @Indexed(unique = true) String recordId,
        String patientId,
        String doctorId,
        String appointmentId,
        Instant visitDate,
        List<String> symptoms,
        String diagnosis,
        String treatment,
        Vitals vitals,
        String notes,
        Instant createdAt) {
    public record Vitals(String bloodPressure, double temperature, int heartRate) { }
}
