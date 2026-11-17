package com.securecare.service;
import com.securecare.service.pricing.StandardAppointmentPricing;import org.junit.jupiter.api.Test;import java.util.Map;import java.math.BigDecimal;import static org.junit.jupiter.api.Assertions.*;
class StandardAppointmentPricingTest{
 @Test void calculate_thirtyMinutes_expectedTwentyFive(){assertEquals(new BigDecimal("25.00"),new StandardAppointmentPricing().calculate(Map.of("durationMinutes",30)));}
 @Test void calculate_sixtyMinutes_expectedFifty(){assertEquals(new BigDecimal("50.00"),new StandardAppointmentPricing().calculate(Map.of("durationMinutes",60)));}
}
