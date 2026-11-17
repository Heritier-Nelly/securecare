package com.securecare.service.pricing;
import java.math.BigDecimal;import java.util.Map;
public interface PricingStrategy{BigDecimal calculate(Map<String,Object> appointment);}
