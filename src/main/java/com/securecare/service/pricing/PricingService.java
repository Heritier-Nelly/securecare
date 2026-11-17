package com.securecare.service.pricing;
import org.springframework.stereotype.Service;import java.math.BigDecimal;import java.util.Map;
@Service public class PricingService{private final PricingStrategy strategy;public PricingService(PricingStrategy strategy){this.strategy=strategy;}public BigDecimal price(Map<String,Object> appointment){return strategy.calculate(appointment);}}
