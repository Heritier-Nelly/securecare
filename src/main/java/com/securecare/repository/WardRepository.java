package com.securecare.repository;

import com.securecare.domain.model.WardDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WardRepository extends MongoRepository<WardDocument, String> { }
