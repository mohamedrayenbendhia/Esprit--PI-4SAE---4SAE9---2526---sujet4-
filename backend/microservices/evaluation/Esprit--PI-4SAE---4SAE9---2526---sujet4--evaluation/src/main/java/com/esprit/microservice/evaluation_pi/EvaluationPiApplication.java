package com.esprit.microservice.evaluation_pi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class EvaluationPiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvaluationPiApplication.class, args);
    }

}
