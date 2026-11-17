package com.securecare.repository;

import com.securecare.domain.model.AdmissionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AdmissionRepository extends MongoRepository<AdmissionDocument, String> { }
