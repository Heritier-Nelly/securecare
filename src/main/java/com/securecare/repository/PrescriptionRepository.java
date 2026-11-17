package com.securecare.repository;

import com.securecare.domain.model.PrescriptionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrescriptionRepository extends MongoRepository<PrescriptionDocument, String> { }
