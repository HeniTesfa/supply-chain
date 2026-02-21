package com.supplychain.suppliersupply.controller;

import com.supplychain.suppliersupply.entity.SupplierSupplyEntity;
import com.supplychain.suppliersupply.repository.SupplierSupplyRepository;
import com.supplychain.suppliersupply.service.SupplierSupplyProcessingService;
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
public class SupplierSupplyController {

    private static final Logger logger = LoggerFactory.getLogger(SupplierSupplyController.class);

    @Autowired
    private SupplierSupplyProcessingService processingService;

    @Autowired
    private SupplierSupplyRepository supplierSupplyRepository;

    // ── Write (called by loader-service) ─────────────────────────────────────

    @PostMapping("/process")
    public ResponseEntity<String> processSupplierSupply(@RequestBody Map<String, Object> supplierSupplyEvent) {
        try {
            logger.info("Received supplier supply event for processing");
            processingService.processSupplierSupply(supplierSupplyEvent);
            return ResponseEntity.ok("Supplier supply processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process supplier supply: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process supplier supply: " + e.getMessage());
        }
    }

    // ── Read (exposed via BFF to frontends) ──────────────────────────────────

    @GetMapping("/supplier-supply")
    public ResponseEntity<List<SupplierSupplyEntity>> getAllSupplierSupply() {
        return ResponseEntity.ok(supplierSupplyRepository.findAll());
    }

    @GetMapping("/supplier-supply/{id}")
    public ResponseEntity<SupplierSupplyEntity> getSupplierSupplyById(@PathVariable String id) {
        return supplierSupplyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/supplier-supply/sku/{skuId}")
    public ResponseEntity<List<SupplierSupplyEntity>> getSupplierSupplyBySkuId(@PathVariable String skuId) {
        return ResponseEntity.ok(supplierSupplyRepository.findBySkuId(skuId));
    }

    @GetMapping("/supplier-supply/warehouse/{warehouseId}")
    public ResponseEntity<List<SupplierSupplyEntity>> getSupplierSupplyByWarehouse(@PathVariable String warehouseId) {
        return ResponseEntity.ok(supplierSupplyRepository.findByWarehouseId(warehouseId));
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "supplier-supply-service"));
    }
}
