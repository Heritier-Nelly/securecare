package com.securecare.service;

import com.securecare.domain.enums.UserRole;
import com.securecare.domain.model.UserDocument;
import com.securecare.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class ResourceService {
    private static final Set<String> PATIENT_RESOURCES = Set.of(
            "patients", "appointments", "prescriptions", "medical-records", "admissions", "billing");
    private static final Set<String> DOCTOR_RESOURCES = Set.of(
            "patients", "appointments", "prescriptions", "medical-records");

    private final MongoTemplate mongo;
    private final UserRepository users;
    private final AuditService audit;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final AppointmentStateMachine stateMachine;
    private final ResourceValidator validator;
    private final AppointmentFactory appointmentFactory;
    private final ResourceCollectionRegistry collections;
    private final ResourceAccessPolicy access;
    private final ResourceSanitizer sanitizer;

    public ResourceService(
            MongoTemplate mongo,
            UserRepository users,
            AuditService audit,
            org.springframework.context.ApplicationEventPublisher events,
            AppointmentStateMachine stateMachine,
            ResourceValidator validator,
            AppointmentFactory appointmentFactory,
            ResourceCollectionRegistry collections,
            ResourceAccessPolicy access,
            ResourceSanitizer sanitizer) {
        this.mongo = mongo;
        this.users = users;
        this.audit = audit;
        this.events = events;
        this.stateMachine = stateMachine;
        this.validator = validator;
        this.appointmentFactory = appointmentFactory;
        this.collections = collections;
        this.access = access;
        this.sanitizer = sanitizer;
    }

    public List<Map<String, Object>> list(String resource) {
        String collection = collections.resolve(resource);
        access.authorize(resource, "READ");
        Criteria ownership = ownershipCriteria(resource);
        List<Document> records = ownership == null
                ? mongo.findAll(Document.class, collection)
                : mongo.find(Query.query(ownership), Document.class, collection);
        return new ArrayList<>(records);
    }

    public Map<String, Object> get(String resource, String id) {
        String collection = collections.resolve(resource);
        access.authorize(resource, "READ");
        Criteria ownership = ownershipCriteria(resource);
        Map<String, Object> found = findById(collection, id, ownership);
        if (found == null) {
            if (ownership != null && findById(collection, id, null) != null) {
                throw new AccessDeniedException("You do not have access to this record");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }
        return found;
    }

    public Map<String, Object> create(String resource, Map<String, Object> body, HttpServletRequest request) {
        String collection = collections.resolve(resource);
        access.authorize(resource, "CREATE");
        UserDocument user = scopedUser(resource);
        Map<String, Object> doc = resource.equals("appointments")
                ? appointmentFactory.create(body, String.valueOf(SecurityContextHolder.getContext().getAuthentication().getDetails()))
                : new LinkedHashMap<>(body);

        if (user != null && user.role() == UserRole.DOCTOR
                && (resource.equals("medical-records") || resource.equals("prescriptions"))) {
            String patientId = stringValue(doc.get("patientId"));
            if (patientId == null || !assignedPatientIds(user.staffId()).contains(patientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create records for assigned patients");
            }
            doc.put("doctorId", user.staffId());
        }

        doc.put("_id", UUID.randomUUID().toString());
        doc.remove("id");
        validator.validate(resource, doc);
        Instant now = Instant.now();
        doc.putIfAbsent("createdAt", now.toString());
        doc.put("updatedAt", now.toString());

        Map<String, Object> saved = mongo.insert(doc, collection);
        audit.record("CREATE_" + resource.toUpperCase(Locale.ROOT), resource, String.valueOf(saved.get("_id")),
                Map.of("new", sanitizer.sanitize(saved)), request);
        return saved;
    }

    public Map<String, Object> update(String resource, String id, Map<String, Object> body, HttpServletRequest request) {
        String collection = collections.resolve(resource);
        access.authorize(resource, "UPDATE");
        Criteria ownership = ownershipCriteria(resource);
        Map<String, Object> old = findById(collection, id, ownership);
        if (old == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }

        UserDocument user = scopedUser(resource);
        if (user != null && user.role() == UserRole.DOCTOR
                && (body.containsKey("patientId") || body.containsKey("doctorId"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot reassign a record to another owner");
        }

        Map<String, Object> next = new LinkedHashMap<>(old);
        body.forEach((key, value) -> {
            if (!key.equals("_id") && !key.equals("id") && !key.equals("createdAt")) {
                next.put(key, value);
            }
        });
        if (resource.equals("appointments") && body.containsKey("status")) {
            stateMachine.assertTransition(String.valueOf(old.get("status")), String.valueOf(body.get("status")));
        }
        validator.validate(resource, next);
        next.put("updatedAt", Instant.now().toString());
        mongo.save(next, collection);

        audit.record("UPDATE_" + resource.toUpperCase(Locale.ROOT), resource, id,
                Map.of("old", sanitizer.sanitize(old), "new", sanitizer.sanitize(next)), request);
        if (resource.equals("appointments") && body.containsKey("status")) {
            events.publishEvent(new AppointmentStatusChangedEvent(id, String.valueOf(old.get("status")),
                    String.valueOf(next.get("status")),
                    String.valueOf(SecurityContextHolder.getContext().getAuthentication().getDetails())));
        }
        return next;
    }

    public void delete(String resource, String id, HttpServletRequest request) {
        String collection = collections.resolve(resource);
        access.authorize(resource, "DELETE");
        Criteria ownership = ownershipCriteria(resource);
        Map<String, Object> old = findById(collection, id, ownership);
        if (old == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }
        mongo.remove(queryForIdAndOwnership(id, ownership), collection);
        audit.record("DELETE_" + resource.toUpperCase(Locale.ROOT), resource, id,
                Map.of("old", sanitizer.sanitize(old)), request);
    }

    private Criteria ownershipCriteria(String resource) {
        UserDocument user = scopedUser(resource);
        if (user == null) {
            return null;
        }
        if (user.role() == UserRole.PATIENT) {
            String patientId = requireLink(user.patientId(), "patientId");
            return Criteria.where("patientId").is(patientId);
        }

        String staffId = requireLink(user.staffId(), "staffId");
        if (resource.equals("appointments")) {
            return Criteria.where("doctorId").is(staffId);
        }

        Set<String> assignedPatients = assignedPatientIds(staffId);
        Criteria patientScope = Criteria.where("patientId").in(assignedPatients);
        if (resource.equals("patients")) {
            return patientScope;
        }
        return new Criteria().andOperator(Criteria.where("doctorId").is(staffId), patientScope);
    }

    private UserDocument scopedUser(String resource) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        boolean patientScope = hasRole(authentication, UserRole.PATIENT) && PATIENT_RESOURCES.contains(resource);
        boolean doctorScope = hasRole(authentication, UserRole.DOCTOR) && DOCTOR_RESOURCES.contains(resource);
        if (!patientScope && !doctorScope) {
            return null;
        }

        Object principalId = authentication.getDetails();
        if (!(principalId instanceof String id) || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user ID is unavailable");
        }
        UserDocument user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        return user.role() == UserRole.PATIENT || user.role() == UserRole.DOCTOR ? user : null;
    }

    private boolean hasRole(Authentication authentication, UserRole role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    private String requireLink(String value, String linkName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No " + linkName + " is configured for this account");
        }
        return value;
    }

    private Set<String> assignedPatientIds(String staffId) {
        String appointmentsCollection = collections.resolve("appointments");
        List<Document> appointments = mongo.find(
                Query.query(Criteria.where("doctorId").is(staffId)), Document.class, appointmentsCollection);
        Set<String> patientIds = new LinkedHashSet<>();
        for (Map<String, Object> appointment : appointments) {
            String patientId = stringValue(appointment.get("patientId"));
            if (patientId != null) {
                patientIds.add(patientId);
            }
        }
        return patientIds;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String result = String.valueOf(value);
        return result.isBlank() ? null : result;
    }

    private Map<String, Object> findById(String collection, String id, Criteria ownership) {
        return mongo.findOne(queryForIdAndOwnership(id, ownership), Document.class, collection);
    }

    private Query queryForIdAndOwnership(String id, Criteria ownership) {
        Query query = Query.query(Criteria.where("_id").is(id));
        if (ownership != null) {
            query.addCriteria(ownership);
        }
        return query;
    }
}
