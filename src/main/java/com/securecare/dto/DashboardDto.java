package com.securecare.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DashboardDto(
        long patients,
        long doctors,
        long appointments,
        long prescriptions,
        @JsonProperty("medical-records") long medicalRecords,
        long wards,
        long admissions,
        long billing,
        List<AuditLogDto> recentActivity) {
}
