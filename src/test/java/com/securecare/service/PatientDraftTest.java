package com.securecare.service;
import com.securecare.domain.model.PatientDraft;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class PatientDraftTest{
 @Test void build_missingPatientId_expectedFailure(){assertThrows(IllegalStateException.class,()->PatientDraft.builder().name("A Patient").build());}
 @Test void build_validPatient_expectedDraft(){var p=PatientDraft.builder().patientId("P-1").name("A Patient").build();assertEquals("P-1",p.patientId());}
}
