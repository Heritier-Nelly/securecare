package com.securecare.controller;
import com.securecare.service.pricing.PricingService;import org.springframework.web.bind.annotation.*;import java.math.BigDecimal;import java.util.Map;
@RestController @RequestMapping("/api/pricing") public class PricingController{
 private final PricingService pricing;public PricingController(PricingService pricing){this.pricing=pricing;}
 @PostMapping("/appointment") public Map<String,BigDecimal> appointmentPrice(@jakarta.validation.Valid @RequestBody Map<String,Object> request){Object minutes=request.getOrDefault("durationMinutes",30);try{int duration=new BigDecimal(String.valueOf(minutes)).intValueExact();if(duration<5||duration>480)throw new NumberFormatException();}catch(RuntimeException ex){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"durationMinutes must be a whole number between 5 and 480");}return Map.of("estimatedAmount",pricing.price(request));}
}
