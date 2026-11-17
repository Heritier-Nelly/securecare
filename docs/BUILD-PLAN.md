# SecureCare implementation map

1. Foundation: Spring Boot/Maven, MongoDB configuration, environment-based secrets, structured logging.
2. Identity: BCrypt, JWT, stateless request filter, login throttling, admin bootstrap, role checks.
3. Data API: patients, doctors, appointments, prescriptions, medical records, wards, admissions and billing.
4. Governance: audit trail, 90-day TTL, dashboard and global error responses.
5. UI: vanilla JavaScript login, dashboard, data tables, forms, edit/delete controls and responsive layout.
6. Quality: focused JUnit tests and Maven JaCoCo report.

## Important implementation boundaries

- The REST API uses request maps for generic resource CRUD; the domain records remain the canonical document shapes and indexes. For a production clinical deployment, replace generic maps with per-domain validated request/response DTOs and explicit service methods before handling real patient data.
- MongoDB references between collections are identifiers; the API does not currently enforce every cross-collection foreign-key-like relationship or transaction.
- Audit events record the acting user, action, resource, entity ID, timestamp, request IP and user-agent. Password fields are excluded from audit payloads.
- The login limiter is in-memory and per application instance. Use a shared store such as Redis for a multi-instance deployment.
- Logout is client-side token removal. JWT revocation/refresh-token rotation is not implemented.
- The notification observer currently logs the appointment status-change event; connect an approved email/SMS provider for external notifications.
