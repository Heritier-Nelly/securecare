package com.securecare.service;
import org.springframework.stereotype.Component;import java.util.*;
@Component public class ResourceSanitizer {
    public Map<String,Object> sanitize(Map source){Map<String,Object> copy=new LinkedHashMap<>(source);copy.remove("passwordHash");copy.remove("password");return copy;}
}
