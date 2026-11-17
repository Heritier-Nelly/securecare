package com.securecare.controller;
import com.securecare.dto.AuditLogDto;
import com.securecare.repository.AuditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/audit-logs") @PreAuthorize("hasRole('ADMIN')") public class AuditController {
 private final AuditLogRepository logs; public AuditController(AuditLogRepository logs){this.logs=logs;}
 @GetMapping public List<AuditLogDto> list(){return logs.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"timestamp")).stream().map(AuditLogDto::from).toList();}
}
