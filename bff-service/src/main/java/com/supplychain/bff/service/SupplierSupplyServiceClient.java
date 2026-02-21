package com.supplychain.bff.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class SupplierSupplyServiceClient {

    private final WebClient webClient;

    public SupplierSupplyServiceClient(WebClient.Builder builder,
                                       @Value("${services.supplier-supply-service.url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public List<Map> getAllSupplierSupply() {
        return webClient.get().uri("/api/supplier-supply")
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }

    public Map getSupplierSupplyById(String id) {
        return webClient.get().uri("/api/supplier-supply/{id}", id)
                .retrieve().bodyToMono(Map.class).block();
    }

    public List<Map> getSupplierSupplyBySkuId(String skuId) {
        return webClient.get().uri("/api/supplier-supply/sku/{skuId}", skuId)
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }

    public List<Map> getSupplierSupplyByWarehouse(String warehouseId) {
        return webClient.get().uri("/api/supplier-supply/warehouse/{warehouseId}", warehouseId)
                .retrieve().bodyToFlux(Map.class).collectList().block();
    }
}
