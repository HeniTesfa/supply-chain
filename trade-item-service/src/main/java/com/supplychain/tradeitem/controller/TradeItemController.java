package com.supplychain.tradeitem.controller;

import com.supplychain.tradeitem.entity.TradeItemEntity;
import com.supplychain.tradeitem.repository.TradeItemRepository;
import com.supplychain.tradeitem.service.TradeItemProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TradeItemController {

    private static final Logger logger = LoggerFactory.getLogger(TradeItemController.class);

    @Autowired
    private TradeItemProcessingService processingService;

    @Autowired
    private TradeItemRepository tradeItemRepository;

    // ── Write (called by loader-service) ─────────────────────────────────────

    @PostMapping("/process")
    public ResponseEntity<String> processTradeItem(@RequestBody Map<String, Object> tradeItemEvent) {
        try {
            logger.info("Received trade item event for processing");
            processingService.processTradeItem(tradeItemEvent);
            return ResponseEntity.ok("Trade item processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process trade item: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process trade item: " + e.getMessage());
        }
    }

    // ── Read (exposed via BFF to frontends) ──────────────────────────────────

    @GetMapping("/trade-items")
    public ResponseEntity<List<TradeItemEntity>> getAllTradeItems() {
        return ResponseEntity.ok(tradeItemRepository.findAll());
    }

    @GetMapping("/trade-items/{id}")
    public ResponseEntity<TradeItemEntity> getTradeItemById(@PathVariable String id) {
        return tradeItemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trade-items/gtin/{gtin}")
    public ResponseEntity<TradeItemEntity> getTradeItemByGtin(@PathVariable String gtin) {
        return tradeItemRepository.findByGtin(gtin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trade-items/supplier/{supplierId}")
    public ResponseEntity<List<TradeItemEntity>> getTradeItemsBySupplierId(@PathVariable String supplierId) {
        return ResponseEntity.ok(tradeItemRepository.findBySupplierId(supplierId));
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "trade-item-service"));
    }
}
