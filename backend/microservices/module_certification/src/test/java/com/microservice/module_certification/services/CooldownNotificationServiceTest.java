package com.microservice.module_certification.services;

import com.microservice.module_certification.entities.UserTestResult;
import com.microservice.module_certification.repositories.UserTestResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test; // On garde cet import pour @Test
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CooldownNotificationServiceTest {

    @Mock
    private UserTestResultRepository userTestResultRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserClientService userClientService;

    @InjectMocks
    private CooldownNotificationService cooldownNotificationService;

    private UserTestResult mockResult;

    @BeforeEach
    void setUp() {
        mockResult = new UserTestResult();
        mockResult.setUserId("user-789");
        mockResult.setNotificationSent(false);

        // ✅ RÉSOLUTION : Utilisation du nom complet pour ton entité Test
        com.microservice.module_certification.entities.Test testEntity = new com.microservice.module_certification.entities.Test();
        testEntity.setTitle("Angular Expert");
        mockResult.setTest(testEntity);
    }

    @Test // ✅ JUnit utilise maintenant l'import sans ambiguïté
    @DisplayName("Devrait envoyer un email et mettre à jour la base quand le cooldown est expiré")
    void checkExpiredCooldowns_ShouldSendEmailSuccessfully() {
        when(userTestResultRepository.findExpiredCooldowns(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockResult));

        when(userClientService.getUserEmail("user-789")).thenReturn("freelancer@test.com");
        when(userClientService.getUserFirstName("user-789")).thenReturn("Rayen");

        cooldownNotificationService.checkExpiredCooldowns();

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(userTestResultRepository, times(1)).save(mockResult);
        assertTrue(mockResult.isNotificationSent());
    }

    @Test
    @DisplayName("Ne devrait rien envoyer si aucun cooldown n'est trouvé")
    void checkExpiredCooldowns_NoActionWhenEmpty() {
        when(userTestResultRepository.findExpiredCooldowns(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        cooldownNotificationService.checkExpiredCooldowns();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Devrait gérer les erreurs sans bloquer la boucle")
    void checkExpiredCooldowns_ShouldHandleExceptions() {
        when(userTestResultRepository.findExpiredCooldowns(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockResult));

        when(userClientService.getUserEmail(anyString())).thenThrow(new RuntimeException("Service Down"));

        cooldownNotificationService.checkExpiredCooldowns();

        verify(userTestResultRepository, never()).save(mockResult);
    }
}