package com.securecare.dto;

import com.securecare.domain.model.AuditLogDocument;
import java.time.Instant;
import java.util.Map;

public record AuditLogDto(
        String id,
        Instant timestamp,
        String userId,
        String username,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String userAgent,
        Map<String, Object> details) {

    public static AuditLogDto from(AuditLogDocument log) {
        return new AuditLogDto(
                log.id(),
                log.timestamp(),
                log.userId(),
                log.username(),
                log.action(),
                log.entityType(),
                log.entityId(),
                log.ipAddress(),
                log.userAgent(),
                log.details());
    }
}
