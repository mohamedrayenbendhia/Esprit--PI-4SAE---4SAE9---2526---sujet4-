package com.esprit.microservice.contrat_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ContratBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContratBackendApplication.class, args);
    }

}
