package com.supplychain.item.controller;

import com.supplychain.item.entity.ItemEntity;
import com.supplychain.item.repository.ItemRepository;
import com.supplychain.item.service.ItemProcessingService;
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
public class ItemController {

    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemProcessingService itemProcessingService;

    @Autowired
    private ItemRepository itemRepository;

    // ── Write (called by loader-service) ─────────────────────────────────────

    @PostMapping("/process")
    public ResponseEntity<String> processItem(@RequestBody Map<String, Object> itemEvent) {
        try {
            logger.info("Received item event for processing");
            itemProcessingService.processItem(itemEvent);
            return ResponseEntity.ok("Item processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process item: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process item: " + e.getMessage());
        }
    }

    // ── Read (exposed via BFF to frontends) ──────────────────────────────────

    @GetMapping("/items")
    public ResponseEntity<List<ItemEntity>> getAllItems() {
        return ResponseEntity.ok(itemRepository.findAll());
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ItemEntity> getItemById(@PathVariable String id) {
        return itemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/items/sku/{skuId}")
    public ResponseEntity<ItemEntity> getItemBySkuId(@PathVariable String skuId) {
        return itemRepository.findBySkuId(skuId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/items/status/{status}")
    public ResponseEntity<List<ItemEntity>> getItemsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(itemRepository.findByStatus(status));
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "item-service"));
    }
}
