package com.supplychain.bff.controller;

import com.supplychain.bff.service.ProducerServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*")
public class ProducerBffController {

    private static final Logger logger = LoggerFactory.getLogger(ProducerBffController.class);
    private static final Set<String> VALID_EVENT_TYPES = Set.of("item", "trade-item", "supplier-supply", "shipment");

    @Autowired
    private ProducerServiceClient producerServiceClient;

    @PostMapping("/api/events")
    public ResponseEntity<?> publishEvent(@RequestBody Map<String, Object> eventData,
                                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String eventType = (String) eventData.get("eventType");

        if (eventType == null || !VALID_EVENT_TYPES.contains(eventType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing eventType. Must be one of: " + VALID_EVENT_TYPES));
        }

        try {
            Map<String, Object> result = producerServiceClient.publishEvent(eventType, eventData, idempotencyKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error proxying {} event: {}", eventType, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
