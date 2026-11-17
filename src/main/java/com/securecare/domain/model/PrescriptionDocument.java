package com.securecare.domain.model;

import com.securecare.domain.enums.PrescriptionStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("prescriptions")
@CompoundIndex(name = "patient_prescription_status_idx", def = "{'patientId': 1, 'status': 1}")
public record PrescriptionDocument(
        @Id String id,
        @Indexed(unique = true) String prescriptionId,
        String patientId,
        String doctorId,
        String appointmentId,
        Instant issuedAt,
        List<Medication> medications,
        PrescriptionStatus status,
        String notes) {
    public record Medication(String name, String dosage, String frequency, int durationDays) { }
}
