package com.smartfreelance.microservice.organizationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationResponse {
    private String organizationId;
    private String organizationName;
    private String address;
    private Double latitude;
    private Double longitude;
    /** true if latitude and longitude are available */
    private boolean geocoded;
}
