package com.microservice.module_certification.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiQuestionGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionGeneratorService.class);
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Value("${groq.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public record GeneratedQuestion(String questionText, String correctAnswer) {}

    public List<GeneratedQuestion> generateQuestions(String testTitle, String skillName, int count) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("groq.api-key is not configured");
        }

        String prompt = buildPrompt(testTitle, skillName, count);

        Map<String, Object> body = Map.of(
            "model", "llama-3.3-70b-versatile",
            "messages", List.of(
                Map.of("role", "system", "content",
                    "You are a certification exam creator. Always respond with valid JSON only, no markdown, no explanation."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(GROQ_URL, request, String.class);
        return parseResponse(response.getBody(), count);
    }

    private String buildPrompt(String testTitle, String skillName, int count) {
        return String.format(
            "Generate exactly %d certification exam questions about \"%s\" (skill: %s).\n\n" +
            "Return ONLY a JSON array with no extra text:\n" +
            "[\n" +
            "  {\"questionText\": \"What is...?\", \"correctAnswer\": \"The answer\"},\n" +
            "  ...\n" +
            "]\n\n" +
            "Requirements:\n" +
            "- Questions must be technical and relevant\n" +
            "- Correct answer must be concise (1-10 words)\n" +
            "- Return exactly %d items",
            count, testTitle, skillName, count
        );
    }

    private List<GeneratedQuestion> parseResponse(String responseBody, int expected) {
        List<GeneratedQuestion> questions = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(responseBody);
            String text = root
                .path("choices").get(0)
                .path("message")
                .path("content").asText();

            text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            JsonNode array = mapper.readTree(text);
            for (JsonNode node : array) {
                questions.add(new GeneratedQuestion(
                    node.path("questionText").asText(),
                    node.path("correctAnswer").asText()
                ));
            }
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            throw new RuntimeException("AI response could not be parsed: " + e.getMessage());
        }
        return questions;
    }
}
