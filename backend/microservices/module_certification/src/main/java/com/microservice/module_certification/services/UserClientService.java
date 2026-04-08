package com.microservice.module_certification.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserClientService {

    private final RestTemplate restTemplate;

    public String getUserEmail(String userId) {
        try {
            String url = "http://localhost:8084/api/users/internal/" + userId;
            Map<String, String> user = restTemplate.getForObject(url, Map.class);
            if (user != null) return user.get("email");
        } catch (Exception e) {
            log.error("Error fetching email for userId: {}", userId, e);
        }
        return null;
    }

    public String getUserFirstName(String userId) {
        try {
            String url = "http://localhost:8084/api/users/internal/" + userId;
            Map<String, String> user = restTemplate.getForObject(url, Map.class);
            if (user != null) return user.get("firstName");
        } catch (Exception e) {
            log.error("Error fetching firstName for userId: {}", userId, e);
        }
        return "Freelancer";
    }
}