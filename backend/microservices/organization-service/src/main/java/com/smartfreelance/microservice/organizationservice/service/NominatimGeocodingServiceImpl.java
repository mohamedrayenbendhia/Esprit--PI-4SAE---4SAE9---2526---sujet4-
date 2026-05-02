package com.smartfreelance.microservice.organizationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartfreelance.microservice.organizationservice.dto.response.GeoLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NominatimGeocodingServiceImpl implements GeocodingService {

    private final RestTemplate geocodingRestTemplate;

    @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String baseUrl;

    @Value("${geocoding.nominatim.user-agent:SmartFreelance/1.0}")
    private String userAgent;

    @Override
    public Optional<GeoLocation> geocode(String address) {
        if (address == null || address.isBlank()) {
            log.warn("Geocoding called with blank address — skipping");
            return Optional.empty();
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", address)
                    .queryParam("format", "json")
                    .queryParam("limit", "1")
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, userAgent);

            ResponseEntity<JsonNode[]> response = geocodingRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode[].class
            );

            JsonNode[] results = response.getBody();
            if (results == null || results.length == 0) {
                log.info("Nominatim returned no results for address: {}", address);
                return Optional.empty();
            }

            JsonNode first = results[0];
            double lat = first.get("lat").asDouble();
            double lon = first.get("lon").asDouble();

            log.info("Geocoded '{}' → lat={}, lon={}", address, lat, lon);
            return Optional.of(GeoLocation.builder().latitude(lat).longitude(lon).build());

        } catch (Exception e) {
            log.error("Geocoding failed for address '{}': {}", address, e.getMessage());
            return Optional.empty();
        }
    }
}
