package com.securecare.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class ResourceValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResourceValidator.class);
    public void validate(String resource, Map<String, Object> data) {
        List<String> required = switch (resource) {
            case "patients" -> List.of("patientId", "name", "dateOfBirth");
            case "doctors" -> List.of("doctorId", "name", "email", "specialization", "department", "licenseNumber");
            case "appointments" -> List.of("appointmentId", "patientId", "doctorId", "scheduledAt", "durationMinutes", "reason");
            case "prescriptions" -> List.of("prescriptionId", "patientId", "doctorId", "medications");
            case "medical-records" -> List.of("recordId", "patientId", "doctorId", "visitDate", "symptoms", "diagnosis", "treatment");
            case "wards" -> List.of("wardId", "name", "department", "capacity");
            case "admissions" -> List.of("admissionId", "patientId", "wardId", "bedNumber", "admittedAt", "reason");
            case "billing" -> List.of("invoiceId", "patientId", "items", "totalAmount");
            default -> List.of();
        };
        for (String field : required) {
            Object value = data.get(field);
            if (value == null || String.valueOf(value).isBlank()) reject(field + " is required");
        }
        if (resource.equals("doctors")) {
            validateLicense(String.valueOf(data.get("licenseNumber")));
            if (!String.valueOf(data.get("email")).matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) reject("Doctor email is invalid");
        }
        if (resource.equals("patients")) validatePatient(data);
        if (resource.equals("doctors")) validatePhone(String.valueOf(data.getOrDefault("phone", "")));
        if (resource.equals("appointments")) {
            validateInstant(String.valueOf(data.get("scheduledAt")), "scheduledAt");
            int duration = integer(data.getOrDefault("durationMinutes", 30), "durationMinutes");
            if (duration < 5 || duration > 480) reject("durationMinutes must be between 5 and 480");
            data.putIfAbsent("status", "SCHEDULED");
            if (!List.of("SCHEDULED","CONFIRMED","IN_PROGRESS","COMPLETED","CANCELLED","NO_SHOW").contains(String.valueOf(data.get("status")))) reject("Appointment status is invalid");
        }
        if (resource.equals("prescriptions")) { data.putIfAbsent("status", "ACTIVE"); if (!List.of("ACTIVE","COMPLETED","CANCELLED").contains(String.valueOf(data.get("status")))) reject("Prescription status is invalid"); }
        if (resource.equals("medical-records")) validateInstant(String.valueOf(data.get("visitDate")), "visitDate");
        if (resource.equals("admissions")) { data.putIfAbsent("status", "ADMITTED"); if (!List.of("ADMITTED","DISCHARGED").contains(String.valueOf(data.get("status")))) reject("Admission status is invalid"); }
        if (resource.equals("billing")) {
            try { if (new java.math.BigDecimal(String.valueOf(data.get("totalAmount"))).signum() < 0) reject("totalAmount cannot be negative"); }
            catch (NumberFormatException ex) { reject("totalAmount must be a valid number"); }
            data.putIfAbsent("status", "PENDING");
            if (!List.of("PENDING","PAID","CANCELLED").contains(String.valueOf(data.get("status")))) reject("Invoice status is invalid");
        }
        if (resource.equals("wards")) {
            int capacity = integer(data.get("capacity"), "capacity");
            int occupancy = integer(data.getOrDefault("currentOccupancy", 0), "currentOccupancy");
            if (capacity < 1 || occupancy < 0 || occupancy > capacity) reject("Ward occupancy must be between zero and capacity");
        }
    }

    private void validatePatient(Map<String, Object> data) {
        Object date = data.get("dateOfBirth");
        if (date != null) try { if (LocalDate.parse(String.valueOf(date)).isAfter(LocalDate.now())) reject("dateOfBirth cannot be in the future"); }
        catch (java.time.format.DateTimeParseException ex) { reject("dateOfBirth must use YYYY-MM-DD"); }
        Object emergency = data.get("emergencyContact");
        if (emergency instanceof Map<?, ?> details && details.get("phone") != null) validatePhone(String.valueOf(details.get("phone")));
        Object contact = data.get("contact");
        if (contact instanceof Map<?, ?> details) {
            Object phone = details.get("phone"); if (phone != null) validatePhone(String.valueOf(phone));
            Object email = details.get("email"); if (email != null && !String.valueOf(email).matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) reject("contact.email is invalid");
        }
    }
    private void validatePhone(String phone) {
        if (!phone.isBlank() && !phone.matches("^\\+?[0-9() .-]{7,20}$")) reject("Phone number is invalid");
    }
    private void validateLicense(String license) {
        if (!license.matches("[A-Za-z0-9-]{4,30}")) reject("Medical license number is invalid");
    }
    private void validateInstant(String value, String field) {
        try { Instant.parse(value); } catch (RuntimeException ex) { reject(field + " must be an ISO-8601 timestamp"); }
    }
    private int integer(Object value, String field) {
        try { return new java.math.BigDecimal(String.valueOf(value)).intValueExact(); }
        catch (RuntimeException ex) { reject(field + " must be a whole number"); return 0; }
    }
    private void reject(String message) { log.warn("Business validation rejected input: {}", message); throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
