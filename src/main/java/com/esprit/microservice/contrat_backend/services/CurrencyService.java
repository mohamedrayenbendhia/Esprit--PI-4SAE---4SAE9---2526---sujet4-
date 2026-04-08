package com.esprit.microservice.contrat_backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CurrencyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://api.exchangerate-api.com/v4/latest/";

    public Map<String, Object> getRates(String baseCurrency) {
        String url = BASE_URL + baseCurrency.toUpperCase();
        return restTemplate.getForObject(url, Map.class);
    }

    public double convert(double amount, String from, String to) {
        Map<String, Object> response = getRates(from);
        Map<String, Double> rates = (Map<String, Double>) response.get("rates");

        if (!rates.containsKey(to.toUpperCase())) {
            throw new RuntimeException("Devise non supportée : " + to);
        }

        double rate = rates.get(to.toUpperCase());
        return Math.round(amount * rate * 100.0) / 100.0;
    }
}