package com.securecare.service;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class AppointmentStateMachineTest{
 private final AppointmentStateMachine machine=new AppointmentStateMachine();
 @Test void assertTransition_scheduledToConfirmed_expectedSuccess(){assertDoesNotThrow(()->machine.assertTransition("SCHEDULED","CONFIRMED"));}
 @Test void assertTransition_completedToScheduled_expectedFailure(){assertThrows(IllegalStateException.class,()->machine.assertTransition("COMPLETED","SCHEDULED"));}
 @Test void assertTransition_inProgressToCompleted_expectedSuccess(){assertDoesNotThrow(()->machine.assertTransition("IN_PROGRESS","COMPLETED"));}
}
