package com.supplychain.bff.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class TradeItemServiceClient {

    private final WebClient webClient;

    public TradeItemServiceClient(WebClient.Builder builder,
                                  @Value("${services.trade-item-service.url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public List<Map> getAllTradeItems() {
        return webClient.get().uri("/api/trade-items")
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }

    public Map getTradeItemById(String id) {
        return webClient.get().uri("/api/trade-items/{id}", id)
                .retrieve().bodyToMono(Map.class).block();
    }

    public Map getTradeItemByGtin(String gtin) {
        return webClient.get().uri("/api/trade-items/gtin/{gtin}", gtin)
                .retrieve().bodyToMono(Map.class).block();
    }

    public List<Map> getTradeItemsBySupplierId(String supplierId) {
        return webClient.get().uri("/api/trade-items/supplier/{supplierId}", supplierId)
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }
}
