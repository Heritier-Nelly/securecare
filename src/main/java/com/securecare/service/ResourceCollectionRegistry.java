package com.securecare.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Component
public class ResourceCollectionRegistry {
    private static final Map<String,String> COLLECTIONS=Map.of("patients","patients","doctors","doctors","appointments","appointments",
            "prescriptions","prescriptions","medical-records","medical_records","wards","wards","admissions","admissions","billing","billing");
    public String resolve(String resource) {
        String collection=COLLECTIONS.get(resource);
        if(collection==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unknown resource");
        return collection;
    }
}
