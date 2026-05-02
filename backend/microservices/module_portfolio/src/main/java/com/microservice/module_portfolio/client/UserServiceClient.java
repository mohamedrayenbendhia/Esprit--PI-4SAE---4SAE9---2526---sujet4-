package com.microservice.module_portfolio.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "service-user", path = "/api/users")
public interface UserServiceClient {

    @GetMapping("/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);
}
