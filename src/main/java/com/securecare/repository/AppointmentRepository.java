package com.securecare.repository;

import com.securecare.domain.model.AppointmentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppointmentRepository extends MongoRepository<AppointmentDocument, String> { }
