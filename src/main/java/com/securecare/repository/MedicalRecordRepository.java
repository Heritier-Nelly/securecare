package com.securecare.repository;

import com.securecare.domain.model.MedicalRecordDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MedicalRecordRepository extends MongoRepository<MedicalRecordDocument, String> { }
