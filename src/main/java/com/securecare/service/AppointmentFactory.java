package com.securecare.service;
import org.springframework.stereotype.Component;import java.time.Instant;import java.util.*;
@Component public class AppointmentFactory{
 public Map<String,Object> create(Map<String,Object> input,String creator){Map<String,Object> appointment=new LinkedHashMap<>(input);appointment.putIfAbsent("status","SCHEDULED");appointment.put("createdBy",creator);appointment.putIfAbsent("createdAt",Instant.now().toString());appointment.put("updatedAt",Instant.now().toString());return appointment;}
}
