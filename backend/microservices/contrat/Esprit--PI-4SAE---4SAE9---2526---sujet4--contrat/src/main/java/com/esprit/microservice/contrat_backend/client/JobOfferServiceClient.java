package com.esprit.microservice.contrat_backend.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "service-joboffer", path = "/api/job-offers")
public interface JobOfferServiceClient {

    @GetMapping("/{id}")
    Map<String, Object> getJobOfferById(@PathVariable("id") Long id);
}
