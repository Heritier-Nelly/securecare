package com.securecare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Component
public class ResourceAccessPolicy {
    private static final Logger log = LoggerFactory.getLogger(ResourceAccessPolicy.class);

    public void authorize(String resource, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You must be signed in");
        }

        Set<String> roles = new HashSet<>();
        auth.getAuthorities().forEach(a -> roles.add(a.getAuthority()));

        // ADMIN can do everything
        if (roles.contains("ROLE_ADMIN")) return;

        boolean allowed = switch (resource) {
            case "patients" -> action.equals("READ") 
                ? roles.stream().anyMatch(Set.of("ROLE_DOCTOR", "ROLE_NURSE", "ROLE_RECEPTIONIST", "ROLE_PATIENT")::contains) 
                : (roles.contains("ROLE_RECEPTIONIST")) && !action.equals("DELETE");
            
            case "doctors" -> action.equals("READ") 
                ? true 
                : (roles.contains("ROLE_RECEPTIONIST")) && !action.equals("DELETE");
            
            case "wards" -> action.equals("READ") 
                ? true 
                : false; // Only Admin can modify wards
            
            case "appointments" -> action.equals("READ") 
                ? !roles.isEmpty() 
                : (roles.contains("ROLE_RECEPTIONIST") || roles.contains("ROLE_DOCTOR")) && !action.equals("DELETE");
            
            case "prescriptions" -> action.equals("READ") 
                ? roles.stream().anyMatch(Set.of("ROLE_DOCTOR", "ROLE_NURSE", "ROLE_RECEPTIONIST", "ROLE_PATIENT")::contains) 
                : (roles.contains("ROLE_DOCTOR")) && !action.equals("DELETE");
            
            // NURSE ward ownership deferred to Phase 3 — requires wardId on UserDocument.
            case "medical-records" -> action.equals("READ") 
                ? roles.stream().anyMatch(Set.of("ROLE_DOCTOR", "ROLE_NURSE", "ROLE_PATIENT")::contains) 
                : (roles.contains("ROLE_DOCTOR") || roles.contains("ROLE_NURSE")) && !action.equals("DELETE");
            
            case "admissions" -> action.equals("READ") 
                ? roles.stream().anyMatch(Set.of("ROLE_NURSE", "ROLE_RECEPTIONIST", "ROLE_PATIENT")::contains) 
                : (roles.contains("ROLE_NURSE") || roles.contains("ROLE_RECEPTIONIST")) && !action.equals("DELETE");
            
            case "billing" -> action.equals("READ") 
                ? roles.stream().anyMatch(Set.of("ROLE_RECEPTIONIST", "ROLE_PATIENT")::contains) 
                : (roles.contains("ROLE_RECEPTIONIST")) && !action.equals("DELETE");
            
            default -> false;
        };

        if (!allowed) {
            log.warn("Authorization denied username={} resource={} action={}", auth.getName(), resource, action);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role cannot perform this operation");
        }
    }
}
