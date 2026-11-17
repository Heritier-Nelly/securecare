package com.securecare.domain.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("doctors")
@CompoundIndex(name = "department_active_idx", def = "{'department': 1, 'active': 1}")
public record DoctorDocument(
        @Id String id,
        @Indexed(unique = true) String doctorId,
        String name,
        String email,
        String phone,
        String specialization,
        String department,
        @Indexed(unique = true) String licenseNumber,
        List<AvailableHours> availableHours,
        boolean active,
        Instant createdAt) {
    public record AvailableHours(String day, LocalTime start, LocalTime end) { }
}
