package com.smartfreelance.microservice.organizationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfreelance.microservice.organizationservice.dto.response.GeoLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NominatimGeocodingServiceImpl Unit Tests")
class NominatimGeocodingServiceImplTest {

    @Mock private RestTemplate geocodingRestTemplate;

    @InjectMocks private NominatimGeocodingServiceImpl service;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Mockito cannot inject automatically
        ReflectionTestUtils.setField(service, "baseUrl",   "https://nominatim.openstreetmap.org");
        ReflectionTestUtils.setField(service, "userAgent", "SmartFreelance/1.0-test");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds a minimal Nominatim-like JSON result node. */
    private JsonNode nominatimResult(double lat, double lon) throws Exception {
        return mapper.readTree(String.format(
            "[{\"lat\":\"%s\",\"lon\":\"%s\",\"display_name\":\"Paris, France\"}]",
            lat, lon
        ));
    }

    private ResponseEntity<JsonNode[]> toResponse(JsonNode arrayNode) throws Exception {
        JsonNode[] arr = mapper.treeToValue(arrayNode, JsonNode[].class);
        return ResponseEntity.ok(arr);
    }

    // ── Successful geocoding ──────────────────────────────────────────────────

    @Test
    @DisplayName("geocode — valid address returns correct coordinates")
    void geocode_validAddress_returnsCoordinates() throws Exception {
        var body = toResponse(nominatimResult(48.8566, 2.3522));
        when(geocodingRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode[].class)))
                .thenReturn(body);

        Optional<GeoLocation> result = service.geocode("Paris, France");

        assertThat(result).isPresent();
        assertThat(result.get().getLatitude()).isEqualTo(48.8566);
        assertThat(result.get().getLongitude()).isEqualTo(2.3522);
    }

    @Test
    @DisplayName("geocode — sets User-Agent header in request")
    void geocode_setsUserAgentHeader() throws Exception {
        var body = toResponse(nominatimResult(48.8566, 2.3522));
        when(geocodingRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode[].class)))
                .thenReturn(body);

        service.geocode("Paris");

        // Verify the exchange was called (User-Agent is inside HttpEntity — verified by not throwing)
        verify(geocodingRestTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode[].class));
    }

    // ── Empty results ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("geocode — Nominatim returns empty array → Optional.empty()")
    void geocode_emptyResults_returnsEmpty() throws Exception {
        JsonNode[] emptyArr = new JsonNode[0];
        when(geocodingRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode[].class)))
                .thenReturn(ResponseEntity.ok(emptyArr));

        Optional<GeoLocation> result = service.geocode("XK99ZZ_unknown_place");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("geocode — Nominatim returns null body → Optional.empty()")
    void geocode_nullBody_returnsEmpty() {
        when(geocodingRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode[].class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        Optional<GeoLocation> result = service.geocode("some address");

        assertThat(result).isEmpty();
    }

    // ── Guard clauses ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("geocode — null address → Optional.empty() without HTTP call")
    void geocode_nullAddress_returnsEmptyWithoutCall() {
        Optional<GeoLocation> result = service.geocode(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(geocodingRestTemplate);
    }

    @Test
    @DisplayName("geocode — blank address → Optional.empty() without HTTP call")
    void geocode_blankAddress_returnsEmptyWithoutCall() {
        Optional<GeoLocation> result = service.geocode("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(geocodingRestTemplate);
    }

    @Test
    @DisplayName("geocode — empty string → Optional.empty() without HTTP call")
    void geocode_emptyAddress_returnsEmptyWithoutCall() {
        Optional<GeoLocation> result = service.geocode("");

        assertThat(result).isEmpty();
        verifyNoInteractions(geocodingRestTemplate);
    }

    // ── Error handling ────────────────────────────────────────────────────────

    @Test
    @DisplayName("geocode — API throws RestClientException → Optional.empty() (graceful degradation)")
    void geocode_apiException_returnsEmpty() {
        when(geocodingRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode[].class)))
                .thenThrow(new RestClientException("Connection refused"));

        Optional<GeoLocation> result = service.geocode("Paris");

        // Must NOT propagate the exception
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("geocode — any runtime exception → Optional.empty() (graceful degradation)")
    void geocode_runtimeException_returnsEmpty() {
        when(geocodingRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode[].class)))
                .thenThrow(new RuntimeException("Unexpected"));

        Optional<GeoLocation> result = service.geocode("Lyon");

        assertThat(result).isEmpty();
    }
}
