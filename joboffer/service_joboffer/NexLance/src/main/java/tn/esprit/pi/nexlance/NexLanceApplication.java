package tn.esprit.pi.nexlance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NexLanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexLanceApplication.class, args);
    }

}
