package com.securecare.service;
import org.junit.jupiter.api.Test;import org.springframework.web.server.ResponseStatusException;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class ResourceValidatorTest {
 private final ResourceValidator validator=new ResourceValidator();
 @Test void validate_missingPatientName_expectedBadRequest(){assertThrows(ResponseStatusException.class,()->validator.validate("patients",new HashMap<>(Map.of("patientId","P-1"))));}
 @Test void validate_invalidDoctorLicense_expectedBadRequest(){assertThrows(ResponseStatusException.class,()->validator.validate("doctors",new HashMap<>(Map.of("doctorId","D-1","name","Doctor","email","doctor@example.test","specialization","GP","department","General","licenseNumber","!"))));}
 @Test void validate_validAppointment_expectedDefaultStatus(){var data=new HashMap<String,Object>(Map.of("appointmentId","A-1","patientId","P-1","doctorId","D-1","scheduledAt","2026-11-05T10:00:00Z","durationMinutes",30,"reason","Check-up"));validator.validate("appointments",data);assertEquals("SCHEDULED",data.get("status"));}
}
