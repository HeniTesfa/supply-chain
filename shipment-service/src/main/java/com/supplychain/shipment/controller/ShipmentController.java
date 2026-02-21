package com.supplychain.shipment.controller;

import com.supplychain.shipment.entity.ShipmentEntity;
import com.supplychain.shipment.repository.ShipmentRepository;
import com.supplychain.shipment.service.ShipmentProcessingService;
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
public class ShipmentController {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentController.class);

    @Autowired
    private ShipmentProcessingService processingService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    // ── Write (called by loader-service) ─────────────────────────────────────

    @PostMapping("/process")
    public ResponseEntity<String> processShipment(@RequestBody Map<String, Object> shipmentEvent) {
        try {
            logger.info("Received shipment event for processing");
            processingService.processShipment(shipmentEvent);
            return ResponseEntity.ok("Shipment processed successfully");
        } catch (Exception e) {
            logger.error("Failed to process shipment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to process shipment: " + e.getMessage());
        }
    }

    // ── Read (exposed via BFF to frontends) ──────────────────────────────────

    @GetMapping("/shipments")
    public ResponseEntity<List<ShipmentEntity>> getAllShipments() {
        return ResponseEntity.ok(shipmentRepository.findAll());
    }

    @GetMapping("/shipments/{id}")
    public ResponseEntity<ShipmentEntity> getShipmentById(@PathVariable String id) {
        return shipmentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/shipments/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentEntity> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/shipments/status/{status}")
    public ResponseEntity<List<ShipmentEntity>> getShipmentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(shipmentRepository.findByShipmentStatus(status));
    }

    @GetMapping("/shipments/order/{orderId}")
    public ResponseEntity<List<ShipmentEntity>> getShipmentsByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(shipmentRepository.findByOrderId(orderId));
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "shipment-service"));
    }
}
