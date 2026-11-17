package com.securecare.controller;

import com.securecare.dto.ResourceRecordDto;
import com.securecare.service.ResourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api") @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()") public class ResourceController {
    private final ResourceService service; public ResourceController(ResourceService service){this.service=service;}
    @GetMapping("/{resource:patients|doctors|appointments|prescriptions|medical-records|wards|admissions|billing}") public List<ResourceRecordDto> list(@PathVariable String resource){return service.list(resource).stream().map(ResourceRecordDto::new).toList();}
    @GetMapping("/{resource:patients|doctors|appointments|prescriptions|medical-records|wards|admissions|billing}/{id}") public ResourceRecordDto get(@PathVariable String resource,@PathVariable String id){return new ResourceRecordDto(service.get(resource,id));}
    @PostMapping("/{resource:patients|doctors|appointments|prescriptions|medical-records|wards|admissions|billing}") public ResourceRecordDto create(@PathVariable String resource,@Valid @RequestBody Map<String,Object> body,HttpServletRequest request){return new ResourceRecordDto(service.create(resource,body,request));}
    @PutMapping("/{resource:patients|doctors|appointments|prescriptions|medical-records|wards|admissions|billing}/{id}") public ResourceRecordDto update(@PathVariable String resource,@PathVariable String id,@Valid @RequestBody Map<String,Object> body,HttpServletRequest request){return new ResourceRecordDto(service.update(resource,id,body,request));}
    @DeleteMapping("/{resource:patients|doctors|appointments|prescriptions|medical-records|wards|admissions|billing}/{id}") public Map<String,Object> delete(@PathVariable String resource,@PathVariable String id,HttpServletRequest request){service.delete(resource,id,request);return Map.of("message","Record deleted successfully");}
}
