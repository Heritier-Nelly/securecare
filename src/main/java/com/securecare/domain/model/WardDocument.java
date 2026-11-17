package com.securecare.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document("wards")
public record WardDocument(
        @Id String id,
        @Indexed(unique = true) String wardId,
        String name,
        String department,
        int capacity,
        int currentOccupancy,
        List<Bed> beds) {
    public record Bed(String bedNumber, boolean occupied, String patientId) { }
}
