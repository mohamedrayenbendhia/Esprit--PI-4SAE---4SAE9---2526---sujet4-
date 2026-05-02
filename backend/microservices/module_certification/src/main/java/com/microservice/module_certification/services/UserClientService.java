package com.microservice.module_certification.services;

import com.microservice.module_certification.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserClientService {

    private final UserServiceClient userServiceClient;

    private Map<String, String> fetchUser(String userId) {
        try {
            return userServiceClient.getUserInternal(userId);
        } catch (Exception e) {
            log.error("Failed to fetch user data for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public String getUserEmail(String userId) {
        Map<String, String> user = fetchUser(userId);
        return (user != null) ? user.get("email") : null;
    }

    public String getUserFirstName(String userId) {
        Map<String, String> user = fetchUser(userId);
        return (user != null) ? user.getOrDefault("firstName", "Freelancer") : "Freelancer";
    }

    public String getUserPhone(String userId) {
        Map<String, String> user = fetchUser(userId);
        return (user != null) ? user.get("phoneNumber") : null;
    }
}
