package com.microservice.module_certification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling    // <--- CRUCIAL : Active le moteur de tâches planifiées
@EnableFeignClients   // <--- CRUCIAL : Permet d'appeler UserClientService
@EnableDiscoveryClient
public class ModuleCertificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModuleCertificationApplication.class, args);
    }
}