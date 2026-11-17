package com.securecare.service;
public record AppointmentStatusChangedEvent(String appointmentId,String previousStatus,String newStatus,String changedBy){}
