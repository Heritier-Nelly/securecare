package com.securecare.repository;

import com.securecare.domain.model.BillingDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BillingRepository extends MongoRepository<BillingDocument, String> { }
