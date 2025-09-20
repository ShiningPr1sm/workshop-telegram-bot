package com.shiningpr1sm.feedbackbot.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiningpr1sm.feedbackbot.model.FeedbackSentiment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAIService {

    private final WebClient webClient;
    private final String openAiModel;
    private final ObjectMapper objectMapper;

    public OpenAIService(@Value("${openai.api.url}") String openAiApiUrl,
                         @Value("${openai.api.key}") String openAiApiKey,
                         @Value("${openai.model}") String openAiModel,
                         ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(openAiApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.openAiModel = openAiModel;
        this.objectMapper = objectMapper;
    }

    public Mono<AnalysisResult> analyzeFeedback(String feedbackText) {
        String prompt = buildPrompt(feedbackText);
        OpenAIRequest request = OpenAIRequest.builder()
                .model(openAiModel)
                .addMessage(new Message("system", "You are an assistant that analyzes anonymous employee feedback for an auto service. Provide sentiment, criticality, and a brief resolution suggestion in JSON format. Ensure all values are present. Sentiment must be POSITIVE, NEUTRAL, or NEGATIVE. Criticality is 1-5."))
                .addMessage(new Message("user", prompt))
                .responseFormat(new ResponseFormat("json_object"))
                .build();

        return webClient.post()
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(String.class)
                // Парсим ответ
                .map(this::parseOpenAIResponse)
                // Обрабатываем 429 с повторной попыткой
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(throwable -> {
                            String msg = throwable.getMessage();
                            return msg != null && msg.contains("429");
                        })
                )
                .onErrorResume(e -> {
                    System.err.println("Error calling OpenAI API: " + e.getMessage());
                    return Mono.just(AnalysisResult.builder()
                            .sentiment(FeedbackSentiment.NEUTRAL)
                            .criticalityLevel(1)
                            .resolutionSuggestion("Failed to analyze feedback due to API error.")
                            .build());
                });
    }


    private String buildPrompt(String feedbackText) {
        return "Analyze the following employee feedback from an auto service and output a JSON object with 'sentiment' (POSITIVE, NEUTRAL, NEGATIVE), 'criticalityLevel' (1-5), and 'resolutionSuggestion' (a brief plan on how to resolve the issue). Ensure the JSON is valid.\n\n" +
                "Feedback: \"" + feedbackText + "\". Give an output in Ukrainian language.";
    }

    private AnalysisResult parseOpenAIResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                JsonNode contentNode = messageNode.path("content");

                if (contentNode.isTextual()) {
                    String contentString = contentNode.asText();
                    JsonNode analysisNode = objectMapper.readTree(contentString);

                    String sentimentStr = analysisNode.path("sentiment").asText("NEUTRAL");
                    int criticality = analysisNode.path("criticalityLevel").asInt(1);
                    String resolution = analysisNode.path("resolutionSuggestion").asText("No specific resolution suggested.");

                    return AnalysisResult.builder()
                            .sentiment(FeedbackSentiment.valueOf(sentimentStr.toUpperCase()))
                            .criticalityLevel(criticality)
                            .resolutionSuggestion(resolution)
                            .build();
                }
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing OpenAI JSON response: " + e.getMessage());
        }
        return AnalysisResult.builder()
                .sentiment(FeedbackSentiment.NEUTRAL)
                .criticalityLevel(1)
                .resolutionSuggestion("Failed to parse OpenAI analysis.")
                .build();
    }

    @Data
    @Builder
    public static class AnalysisResult {
        private FeedbackSentiment sentiment;
        private Integer criticalityLevel;
        private String resolutionSuggestion;
    }

    @Data
    @Builder
    public static class OpenAIRequest {
        private String model;
        private List<Message> messages;
        @JsonProperty("response_format")
        private ResponseFormat responseFormat;

        public static class OpenAIRequestBuilder {
            private String model;
            private List<Message> messages;
            private ResponseFormat responseFormat;

            public OpenAIRequestBuilder model(String model) {
                this.model = model;
                return this;
            }

            public OpenAIRequestBuilder messages(List<Message> messages) {
                this.messages = messages;
                return this;
            }

            public OpenAIRequestBuilder responseFormat(ResponseFormat responseFormat) {
                this.responseFormat = responseFormat;
                return this;
            }

            public OpenAIRequestBuilder addMessage(Message message) {
                if (this.messages == null) {
                    this.messages = new ArrayList<>();
                }
                this.messages.add(message);
                return this;
            }

            public OpenAIRequest build() {
                return new OpenAIRequest(model, messages, responseFormat);
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseFormat {
        private String type;
    }
}