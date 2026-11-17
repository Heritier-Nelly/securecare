package com.securecare.service;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.context.event.EventListener;import org.springframework.stereotype.Component;
@Component public class AppointmentNotificationListener{private static final Logger log=LoggerFactory.getLogger(AppointmentNotificationListener.class);
 @EventListener public void onStatusChanged(AppointmentStatusChangedEvent event){log.info("Appointment notification: appointmentId={} from={} to={} changedBy={}",event.appointmentId(),event.previousStatus(),event.newStatus(),event.changedBy());}}
