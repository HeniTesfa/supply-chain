package com.supplychain.bff.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ShipmentServiceClient {

    private final WebClient webClient;

    public ShipmentServiceClient(WebClient.Builder builder,
                                 @Value("${services.shipment-service.url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public List<Map> getAllShipments() {
        return webClient.get().uri("/api/shipments")
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }

    public Map getShipmentById(String id) {
        return webClient.get().uri("/api/shipments/{id}", id)
                .retrieve().bodyToMono(Map.class).block();
    }

    public Map getShipmentByTrackingNumber(String trackingNumber) {
        return webClient.get().uri("/api/shipments/tracking/{trackingNumber}", trackingNumber)
                .retrieve().bodyToMono(Map.class).block();
    }

    public List<Map> getShipmentsByStatus(String status) {
        return webClient.get().uri("/api/shipments/status/{status}", status)
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }

    public List<Map> getShipmentsByOrderId(String orderId) {
        return webClient.get().uri("/api/shipments/order/{orderId}", orderId)
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }
}
