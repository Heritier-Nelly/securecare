package com.securecare.repository;

import com.securecare.domain.model.DoctorDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DoctorRepository extends MongoRepository<DoctorDocument, String> { }
