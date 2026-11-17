package com.securecare.domain.model;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("audit_logs")
@CompoundIndex(name = "audit_user_time_idx", def = "{'userId': 1, 'timestamp': -1}")
public record AuditLogDocument(
        @Id String id,
        @Indexed(expireAfter = "90d") Instant timestamp,
        String userId,
        String username,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String userAgent,
        Map<String, Object> details) { }
