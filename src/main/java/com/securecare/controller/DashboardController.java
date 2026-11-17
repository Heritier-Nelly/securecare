package com.securecare.controller;
import com.securecare.domain.model.AuditLogDocument;import com.securecare.dto.AuditLogDto;import com.securecare.dto.DashboardDto;import org.springframework.data.mongodb.core.MongoTemplate;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/dashboard") public class DashboardController{
 private final MongoTemplate mongo;public DashboardController(MongoTemplate mongo){this.mongo=mongo;}
 @GetMapping public DashboardDto summary(){
  var recentQuery=new org.springframework.data.mongodb.core.query.Query().with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"timestamp")).limit(8);
  List<AuditLogDto> recentActivity=mongo.find(recentQuery,AuditLogDocument.class,"audit_logs").stream().map(AuditLogDto::from).toList();
  return new DashboardDto(
   mongo.getCollection("patients").countDocuments(),
   mongo.getCollection("doctors").countDocuments(),
   mongo.getCollection("appointments").countDocuments(),
   mongo.getCollection("prescriptions").countDocuments(),
   mongo.getCollection("medical_records").countDocuments(),
   mongo.getCollection("wards").countDocuments(),
   mongo.getCollection("admissions").countDocuments(),
   mongo.getCollection("billing").countDocuments(),
   recentActivity);
 }
}
