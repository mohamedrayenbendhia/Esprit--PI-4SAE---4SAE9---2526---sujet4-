package tn.esprit.pi.nexlance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "RECOMMENDATION-AI")
public interface RecommendationAiClient {
    @PostMapping("/api/recommendations/sync/job")
    void syncJobToAi(@RequestBody Map<String, Object> jobData);
}