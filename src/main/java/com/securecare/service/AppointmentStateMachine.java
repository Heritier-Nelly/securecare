package com.securecare.service;
import org.springframework.stereotype.Component;import java.util.*;
@Component public class AppointmentStateMachine{
 private static final Map<String,Set<String>> TRANSITIONS=Map.of("SCHEDULED",Set.of("CONFIRMED","CANCELLED","NO_SHOW"),"CONFIRMED",Set.of("IN_PROGRESS","CANCELLED","NO_SHOW"),"IN_PROGRESS",Set.of("COMPLETED"),"COMPLETED",Set.of(),"CANCELLED",Set.of(),"NO_SHOW",Set.of());
 public void assertTransition(String from,String to){if(!TRANSITIONS.getOrDefault(from,Set.of()).contains(to))throw new IllegalStateException("Invalid appointment status transition: "+from+" -> "+to);}
}
