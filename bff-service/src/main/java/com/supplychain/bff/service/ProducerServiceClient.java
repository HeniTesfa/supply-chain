package com.supplychain.bff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class ProducerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ProducerServiceClient.class);
    private final WebClient webClient;

    public ProducerServiceClient(WebClient.Builder webClientBuilder,
                                  @Value("${services.producer-service.url}") String producerServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(producerServiceUrl).build();
    }

    public Map<String, Object> publishEvent(String eventType, Map<String, Object> eventData, String idempotencyKey) {
        logger.debug("Proxying POST /api/producer/{} to producer-service", eventType);

        WebClient.RequestBodySpec request = webClient.post()
                .uri("/api/producer/{eventType}", eventType)
                .header("Content-Type", "application/json");

        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            request = (WebClient.RequestBodySpec) request.header("Idempotency-Key", idempotencyKey);
        }

        return request
                .bodyValue(eventData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
