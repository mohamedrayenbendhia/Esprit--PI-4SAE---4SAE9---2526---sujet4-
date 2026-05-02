package com.microservice.module_certification.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Plus léger et rapide que @RestClientTest
class UserClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserClientService userClientService;

    private final String userId = "user-123";
    private Map<String, String> mockUserResponse;

    @BeforeEach
    void setUp() {
        mockUserResponse = new HashMap<>();
        mockUserResponse.put("email", "rayen@test.com");
        mockUserResponse.put("firstName", "Rayen");
        mockUserResponse.put("phoneNumber", "+21612345678");
    }

    @Test
    @DisplayName("Devrait retourner l'email de l'utilisateur")
    void getUserEmail_ShouldReturnEmail() {
        // Simulation de l'appel RestTemplate
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockUserResponse);

        String email = userClientService.getUserEmail(userId);

        assertEquals("rayen@test.com", email);
    }

    @Test
    @DisplayName("Devrait retourner le prénom")
    void getUserFirstName_ShouldReturnFirstName() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockUserResponse);

        String firstName = userClientService.getUserFirstName(userId);

        assertEquals("Rayen", firstName);
    }

    @Test
    @DisplayName("Devrait retourner le téléphone")
    void getUserPhone_ShouldReturnPhone() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockUserResponse);

        String phone = userClientService.getUserPhone(userId);

        assertEquals("+21612345678", phone);
    }

    @Test
    @DisplayName("Devrait retourner null en cas d'erreur réseau")
    void getUserEmail_ShouldReturnNullOnException() {
        // Simulation d'une exception lors de l'appel
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network Error"));

        String email = userClientService.getUserEmail(userId);

        assertNull(email);
    }

    @Test
    @DisplayName("Devrait retourner 'Freelancer' si le prénom est introuvable après erreur")
    void getUserFirstName_ShouldReturnDefaultOnException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API Down"));

        String firstName = userClientService.getUserFirstName(userId);

        assertEquals("Freelancer", firstName);
    }
}