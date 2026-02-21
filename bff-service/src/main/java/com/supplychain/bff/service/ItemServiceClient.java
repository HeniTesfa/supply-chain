package com.supplychain.bff.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ItemServiceClient {

    private final WebClient webClient;

    public ItemServiceClient(WebClient.Builder builder,
                             @Value("${services.item-service.url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public List<Map> getAllItems() {
        return webClient.get().uri("/api/items")
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }

    public Map getItemById(String id) {
        return webClient.get().uri("/api/items/{id}", id)
                .retrieve().bodyToMono(Map.class).block();
    }

    public Map getItemBySkuId(String skuId) {
        return webClient.get().uri("/api/items/sku/{skuId}", skuId)
                .retrieve().bodyToMono(Map.class).block();
    }

    public List<Map> getItemsByStatus(String status) {
        return webClient.get().uri("/api/items/status/{status}", status)
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }
}
