package com.smartfreelance.microservice.organizationservice.service;

import com.smartfreelance.microservice.organizationservice.dto.response.GeoLocation;

import java.util.Optional;

public interface GeocodingService {

    /**
     * Geocodes the given address using the Nominatim (OpenStreetMap) API.
     *
     * @param address human-readable address string (e.g. "Paris, France")
     * @return an Optional containing latitude/longitude, or empty on failure
     */
    Optional<GeoLocation> geocode(String address);
}
