package com.supplychain.item.service;

import com.supplychain.item.entity.ItemEntity;
import com.supplychain.item.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ItemProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ItemProcessingService.class);

    private final WebClient webClient;

    @Autowired
    private ItemRepository itemRepository;

    @Value("${osp.api.url:http://localhost:9000}")
    private String ospApiUrl;

    @Value("${osp.api.retry.max-attempts:3}")
    private int maxRetryAttempts;

    public ItemProcessingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void processItem(Map<String, Object> itemEvent) {
        logger.info("Processing item event - SKU: {}", itemEvent.get("skuId"));
        validateItemData(itemEvent);
        saveItem(itemEvent);
        Map<String, Object> ospPayload = transformForOspApi(itemEvent);
        sendToOspApi(ospPayload);
        logger.info("Item processing completed successfully");
    }

    private void saveItem(Map<String, Object> itemEvent) {
        String skuId = (String) itemEvent.get("skuId");

        ItemEntity entity = itemRepository.findBySkuId(skuId)
                .orElse(ItemEntity.builder()
                        .skuId(skuId)
                        .createdAt(LocalDateTime.now())
                        .build());

        entity.setItemName((String) itemEvent.get("itemName"));
        entity.setDescription((String) itemEvent.get("description"));
        entity.setStatus((String) itemEvent.getOrDefault("status", "ACTIVE"));
        entity.setUpdatedAt(LocalDateTime.now());

        if (itemEvent.get("price") instanceof Number) {
            entity.setPrice(((Number) itemEvent.get("price")).doubleValue());
        }
        if (itemEvent.get("weight") instanceof Number) {
            entity.setWeight(((Number) itemEvent.get("weight")).doubleValue());
        }

        itemRepository.save(entity);
        logger.info("Item saved to MongoDB - SKU: {}", skuId);
    }

    private void validateItemData(Map<String, Object> itemEvent) {
        if (!itemEvent.containsKey("skuId") || itemEvent.get("skuId") == null) {
            throw new IllegalArgumentException("SKU ID is required");
        }
        String skuId = (String) itemEvent.get("skuId");
        if (skuId.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be empty");
        }
        if (!itemEvent.containsKey("itemName") || itemEvent.get("itemName") == null) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (itemEvent.containsKey("price") && itemEvent.get("price") instanceof Number) {
            if (((Number) itemEvent.get("price")).doubleValue() < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
        }
        if (itemEvent.containsKey("weight") && itemEvent.get("weight") instanceof Number) {
            if (((Number) itemEvent.get("weight")).doubleValue() < 0) {
                throw new IllegalArgumentException("Weight cannot be negative");
            }
        }
    }

    private Map<String, Object> transformForOspApi(Map<String, Object> itemEvent) {
        return itemEvent;
    }

    private void sendToOspApi(Map<String, Object> ospPayload) {
        String skuId = (String) ospPayload.get("skuId");
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            attempt++;
            try {
                logger.info("Sending to OSP API (attempt {}/{}) - SKU: {}", attempt, maxRetryAttempts, skuId);
                String response = webClient.post()
                        .uri(ospApiUrl + "/osp/api/items")
                        .bodyValue(ospPayload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                logger.info("OSP API call successful - Response: {}", response);
                return;
            } catch (Exception e) {
                lastException = e;
                logger.warn("OSP API call failed (attempt {}/{}): {}", attempt, maxRetryAttempts, e.getMessage());
                if (attempt < maxRetryAttempts) {
                    int waitMs = (int) Math.pow(2, attempt) * 1000;
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Failed to send to OSP API after " + maxRetryAttempts + " attempts: " + lastException.getMessage(), lastException);
    }
}
