package com.esprit.microservice.evaluation_pi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "contrat", path = "/contrat_backend/api/contrats")
public interface ContratServiceClient {

    @GetMapping("/{id}")
    Map<String, Object> getContratById(@PathVariable("id") Long id);
}
