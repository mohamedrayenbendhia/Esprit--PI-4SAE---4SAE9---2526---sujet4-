package com.microservice.module_certification.services;

import com.microservice.module_certification.entities.Certification;
import com.microservice.module_certification.repositories.CertificationRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test; // Import corrigé
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificationExpiryServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private UserClientService userClientService;

    @InjectMocks
    private CertificationExpiryService certificationExpiryService;

    @BeforeEach
    void setUp() {
        // Injection des @Value qui sont null en test unitaire
        ReflectionTestUtils.setField(certificationExpiryService, "fromNumber", "+123456789");
        ReflectionTestUtils.setField(certificationExpiryService, "accountSid", "AC_mock_sid");
        ReflectionTestUtils.setField(certificationExpiryService, "authToken", "mock_token");
    }

    @Test
    @DisplayName("Devrait expirer les certifications et simuler l'envoi d'un SMS")
    void checkExpiredCertifications_ShouldProcessExpirations() {
        // 1. Préparation des données
        com.microservice.module_certification.entities.Test testEntity = new com.microservice.module_certification.entities.Test();
        testEntity.setTitle("Java Certification");

        Certification mockCert = new Certification();
        mockCert.setUserId("user-456");
        mockCert.setExpired(false);
        mockCert.setSmsSent(false);
        mockCert.setTest(testEntity);

        when(certificationRepository.findExpiredCertifications(any(LocalDate.class)))
                .thenReturn(List.of(mockCert));

        when(userClientService.getUserPhone("user-456")).thenReturn("+21612345678");

        // 2. Mocking de Twilio (Statique)
        try (MockedStatic<Message> mockedMessage = mockStatic(Message.class)) {
            MessageCreator mockedCreator = mock(MessageCreator.class);

            // On mocke l'appel Message.creator(...).create()
            mockedMessage.when(() -> Message.creator(
                    any(PhoneNumber.class),
                    any(PhoneNumber.class),
                    anyString()
            )).thenReturn(mockedCreator);

            // 3. Exécution
            certificationExpiryService.checkExpiredCertifications();

            // 4. Vérifications
            verify(certificationRepository, times(1)).save(mockCert);
            assertTrue(mockCert.isExpired(), "La certification devrait être marquée expirée");
            assertTrue(mockCert.isSmsSent(), "Le flag SMS envoyé devrait être à true");
        }
    }
}