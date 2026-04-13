package com.esprit.microservice.contrat_backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyConversionService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";

    public Map<String, Object> convert(double amount, String fromCurrency, String toCurrency) {
        String from = fromCurrency != null ? fromCurrency.toUpperCase() : "EUR";
        String to   = toCurrency  != null ? toCurrency.toUpperCase()  : "EUR";

        Map<String, Object> result = new HashMap<>();

        // Même devise → pas d'appel réseau
        if (from.equalsIgnoreCase(to)) {
            result.put("original",       amount);
            result.put("converted",      amount);
            result.put("targetCurrency", to);
            result.put("rate",           1.0);
            return result;
        }

        try {
            Map<String, Object> response = restTemplate.getForObject(API_URL + from, Map.class);

            if (response == null || !response.containsKey("rates")) {
                throw new RuntimeException("No rates returned for currency: " + from);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");

            if (!rates.containsKey(to)) {
                throw new RuntimeException("Currency not supported: " + to);
            }

            // La valeur peut être Integer ou Double selon Jackson
            double rate = ((Number) rates.get(to)).doubleValue();
            double converted = Math.round(amount * rate * 100.0) / 100.0;

            result.put("original",       amount);
            result.put("converted",      converted);
            result.put("targetCurrency", to);
            result.put("rate",           rate);
            return result;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Exchange rate API unreachable: " + e.getMessage());
        }
    }
}