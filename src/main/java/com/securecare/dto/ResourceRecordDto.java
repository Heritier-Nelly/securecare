package com.securecare.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ResourceRecordDto {
    private final Map<String, Object> fields;

    public ResourceRecordDto(Map<?, ?> fields) {
        Map<String, Object> copy = new LinkedHashMap<>();
        fields.forEach((key, value) -> copy.put(String.valueOf(key), value));
        this.fields = Collections.unmodifiableMap(copy);
    }

    @JsonAnyGetter
    public Map<String, Object> getFields() {
        return fields;
    }
}
