package tn.esprit.pi.service_projects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ServiceProjectsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceProjectsApplication.class, args);
    }

}
