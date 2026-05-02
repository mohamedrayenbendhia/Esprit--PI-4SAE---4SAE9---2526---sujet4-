package com.microservice.module_certification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "service-user", path = "/api/users/internal")
public interface UserServiceClient {

    @GetMapping("/{id}")
    Map<String, String> getUserInternal(@PathVariable("id") String id);
}
