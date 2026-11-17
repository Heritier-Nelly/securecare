package com.securecare.domain.model;

import com.securecare.domain.enums.AppointmentStatus;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("appointments")
@CompoundIndex(name = "doctor_schedule_idx", def = "{'doctorId': 1, 'scheduledAt': 1}")
@CompoundIndex(name = "patient_schedule_idx", def = "{'patientId': 1, 'scheduledAt': 1}")
public record AppointmentDocument(
        @Id String id,
        @Indexed(unique = true) String appointmentId,
        String patientId,
        String doctorId,
        Instant scheduledAt,
        int durationMinutes,
        String reason,
        @Indexed AppointmentStatus status,
        String notes,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) { }
