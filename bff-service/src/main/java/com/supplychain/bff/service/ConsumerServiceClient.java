package com.supplychain.bff.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ConsumerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerServiceClient.class);
    private final WebClient webClient;

    public ConsumerServiceClient(WebClient.Builder webClientBuilder,
                                  @Value("${services.consumer-service.url}") String consumerServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(consumerServiceUrl).build();
    }

    public List<Map> getUnresolvedErrors() {
        logger.debug("Proxying GET /api/errors/unresolved to consumer-service");
        return webClient.get()
                .uri("/api/errors/unresolved")
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();
    }

    public Map<String, Object> getErrorStats() {
        logger.debug("Proxying GET /api/errors/stats to consumer-service");
        return webClient.get()
                .uri("/api/errors/stats")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public List<Map> getErrorsByStage(String stage) {
        logger.debug("Proxying GET /api/errors/stage/{} to consumer-service", stage);
        return webClient.get()
                .uri("/api/errors/stage/{stage}", stage)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();
    }

    public List<Map> getErrorsByType(String eventType) {
        logger.debug("Proxying GET /api/errors/type/{} to consumer-service", eventType);
        return webClient.get()
                .uri("/api/errors/type/{eventType}", eventType)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();
    }

    public String resolveError(String id, Map<String, Object> resolution) {
        logger.debug("Proxying PUT /api/errors/{}/resolve to consumer-service", id);
        return webClient.put()
                .uri("/api/errors/{id}/resolve", id)
                .header("Content-Type", "application/json")
                .bodyValue(resolution)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
