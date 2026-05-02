package com.smartfreelance.microservice.organizationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GeocodingConfig {

    @Value("${geocoding.nominatim.timeout-seconds:10}")
    private int timeoutSeconds;

    /**
     * Dedicated RestTemplate for geocoding calls.
     * Named "geocodingRestTemplate" to avoid conflicts with any other RestTemplate bean.
     */
    @Bean(name = "geocodingRestTemplate")
    public RestTemplate geocodingRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);                      // 5 s connect
        factory.setReadTimeout(timeoutSeconds * 1_000);        // configurable read (default 10 s)
        return new RestTemplate(factory);
    }
}
