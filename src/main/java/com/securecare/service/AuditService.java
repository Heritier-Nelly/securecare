package com.securecare.service;

import com.securecare.domain.model.AuditLogDocument;
import com.securecare.repository.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

@Service public class AuditService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repository;
    public AuditService(AuditLogRepository repository){this.repository=repository;}
    public void recordLogin(com.securecare.domain.model.UserDocument user, HttpServletRequest request) {
        repository.save(new AuditLogDocument(null,Instant.now(),user.id(),user.username(),"LOGIN","users",user.id(),
                request==null?"":request.getRemoteAddr(),request==null?"":request.getHeader("User-Agent"),Map.of()));
        log.info("Audit event recorded userId={} action=LOGIN",user.id());
    }
    public void record(String action,String entityType,String entityId,Map<String,Object> details,HttpServletRequest request){
        var auth=SecurityContextHolder.getContext().getAuthentication();
        String username=auth==null?"system":auth.getName(); String userId=auth!=null && auth.getDetails() instanceof String s?s:"system";
        repository.save(new AuditLogDocument(null,Instant.now(),userId,username,action,entityType,entityId,
                request==null?"":request.getRemoteAddr(),request==null?"":request.getHeader("User-Agent"),details));
        log.info("Audit event recorded userId={} action={} entityType={} entityId={}",userId,action,entityType,entityId);
    }
}
