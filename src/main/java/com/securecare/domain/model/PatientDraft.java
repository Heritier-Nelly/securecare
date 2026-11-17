package com.securecare.domain.model;
import java.time.LocalDate;import java.util.*;
/** Builder pattern for assembling a patient intake payload without a long constructor. */
public final class PatientDraft{
 private final String patientId,name;private final LocalDate dateOfBirth;private final List<String> allergies;
 private PatientDraft(Builder b){patientId=b.patientId;name=b.name;dateOfBirth=b.dateOfBirth;allergies=List.copyOf(b.allergies);}
 public String patientId(){return patientId;}public String name(){return name;}public LocalDate dateOfBirth(){return dateOfBirth;}public List<String> allergies(){return allergies;}
 public static Builder builder(){return new Builder();}
 public static final class Builder{private String patientId,name;private LocalDate dateOfBirth;private List<String> allergies=new ArrayList<>();
  public Builder patientId(String v){patientId=v;return this;}public Builder name(String v){name=v;return this;}public Builder dateOfBirth(LocalDate v){dateOfBirth=v;return this;}public Builder allergies(List<String> v){allergies=v==null?new ArrayList<>():new ArrayList<>(v);return this;}
  public PatientDraft build(){if(patientId==null||patientId.isBlank()||name==null||name.isBlank())throw new IllegalStateException("Patient ID and name are required");return new PatientDraft(this);}}
}
