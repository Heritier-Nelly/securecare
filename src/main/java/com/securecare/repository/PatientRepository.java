package com.securecare.repository;

import com.securecare.domain.model.PatientDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientRepository extends MongoRepository<PatientDocument, String> { }
