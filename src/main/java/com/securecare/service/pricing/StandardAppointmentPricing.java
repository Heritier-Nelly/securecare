package com.securecare.service.pricing;
import org.springframework.stereotype.Component;import java.math.BigDecimal;import java.util.Map;
@Component public class StandardAppointmentPricing implements PricingStrategy{
 public BigDecimal calculate(Map<String,Object> appointment){Object duration=appointment.getOrDefault("durationMinutes",30);return new BigDecimal(String.valueOf(duration)).divide(new BigDecimal("30"),2,java.math.RoundingMode.UP).multiply(new BigDecimal("25.00")).setScale(2,java.math.RoundingMode.UP);}
}
