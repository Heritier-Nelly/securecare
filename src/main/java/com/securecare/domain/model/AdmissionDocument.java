package com.securecare.domain.model;

import com.securecare.domain.enums.AdmissionStatus;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("admissions")
@CompoundIndex(name = "patient_admission_status_idx", def = "{'patientId': 1, 'status': 1}")
public record AdmissionDocument(
        @Id String id,
        @Indexed(unique = true) String admissionId,
        String patientId,
        String wardId,
        String bedNumber,
        Instant admittedAt,
        Instant dischargedAt,
        String reason,
        AdmissionStatus status,
        String notes) { }
