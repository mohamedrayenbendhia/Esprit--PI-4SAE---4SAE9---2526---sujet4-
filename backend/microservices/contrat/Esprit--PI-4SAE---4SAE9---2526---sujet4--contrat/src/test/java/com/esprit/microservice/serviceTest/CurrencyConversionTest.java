package com.esprit.microservice.serviceTest;

import com.esprit.microservice.contrat_backend.services.CurrencyConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CurrencyConversionService currencyService;

    @BeforeEach
    void setUp() {
        // Comme RestTemplate est 'final' et instancié avec 'new',
        // on utilise ReflectionTestUtils pour forcer l'injection du mock.
        ReflectionTestUtils.setField(currencyService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Devrait retourner le même montant si les devises sont identiques")
    void shouldReturnSameAmountWhenCurrenciesAreEqual() {
        // Act
        Map<String, Object> result = currencyService.convert(100.0, "EUR", "EUR");

        // Assert
        assertThat(result.get("converted")).isEqualTo(100.0);
        assertThat(result.get("rate")).isEqualTo(1.0);
        // On vérifie qu'aucun appel réseau n'a été fait
        Mockito.verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Devrait convertir correctement le montant via l'API")
    void shouldConvertAmountUsingApi() {
        // Arrange
        double amount = 100.0;
        double mockRate = 1.1; // 1 EUR = 1.1 USD

        Map<String, Object> mockResponse = new HashMap<>();
        Map<String, Object> rates = new HashMap<>();
        rates.put("USD", mockRate);
        mockResponse.put("rates", rates);

        Mockito.when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockResponse);

        // Act
        Map<String, Object> result = currencyService.convert(amount, "EUR", "USD");

        // Assert
        assertThat(result.get("converted")).isEqualTo(110.0);
        assertThat(result.get("targetCurrency")).isEqualTo("USD");
        assertThat(result.get("rate")).isEqualTo(mockRate);
    }

    @Test
    @DisplayName("Devrait lever une exception si la devise n'est pas supportée")
    void shouldThrowExceptionWhenCurrencyNotSupported() {
        // Arrange
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("rates", new HashMap<>()); // Liste de taux vide

        Mockito.when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> currencyService.convert(100.0, "EUR", "XYZ"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Currency not supported");
    }
}