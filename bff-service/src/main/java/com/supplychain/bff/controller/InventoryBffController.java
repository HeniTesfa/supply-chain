package com.supplychain.bff.controller;

import com.supplychain.bff.service.ItemServiceClient;
import com.supplychain.bff.service.ShipmentServiceClient;
import com.supplychain.bff.service.SupplierSupplyServiceClient;
import com.supplychain.bff.service.TradeItemServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Inventory BFF Controller
 *
 * Single entry point for frontends to query supply chain inventory data.
 * Proxies requests to the appropriate downstream service.
 *
 * Items:          GET /api/inventory/items
 * Trade Items:    GET /api/inventory/trade-items
 * Shipments:      GET /api/inventory/shipments
 * Supplier Supply: GET /api/inventory/supplier-supply
 */
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryBffController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryBffController.class);

    @Autowired private ItemServiceClient itemServiceClient;
    @Autowired private TradeItemServiceClient tradeItemServiceClient;
    @Autowired private ShipmentServiceClient shipmentServiceClient;
    @Autowired private SupplierSupplyServiceClient supplierSupplyServiceClient;

    // ── Items ─────────────────────────────────────────────────────────────────

    @GetMapping("/items")
    public ResponseEntity<?> getAllItems() {
        try {
            return ResponseEntity.ok(itemServiceClient.getAllItems());
        } catch (Exception e) {
            logger.error("Error fetching items: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<?> getItemById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(itemServiceClient.getItemById(id));
        } catch (Exception e) {
            logger.error("Error fetching item {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/items/sku/{skuId}")
    public ResponseEntity<?> getItemBySkuId(@PathVariable String skuId) {
        try {
            return ResponseEntity.ok(itemServiceClient.getItemBySkuId(skuId));
        } catch (Exception e) {
            logger.error("Error fetching item by SKU {}: {}", skuId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/items/status/{status}")
    public ResponseEntity<?> getItemsByStatus(@PathVariable String status) {
        try {
            return ResponseEntity.ok(itemServiceClient.getItemsByStatus(status));
        } catch (Exception e) {
            logger.error("Error fetching items by status {}: {}", status, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Trade Items ───────────────────────────────────────────────────────────

    @GetMapping("/trade-items")
    public ResponseEntity<?> getAllTradeItems() {
        try {
            return ResponseEntity.ok(tradeItemServiceClient.getAllTradeItems());
        } catch (Exception e) {
            logger.error("Error fetching trade items: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/trade-items/{id}")
    public ResponseEntity<?> getTradeItemById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(tradeItemServiceClient.getTradeItemById(id));
        } catch (Exception e) {
            logger.error("Error fetching trade item {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/trade-items/gtin/{gtin}")
    public ResponseEntity<?> getTradeItemByGtin(@PathVariable String gtin) {
        try {
            return ResponseEntity.ok(tradeItemServiceClient.getTradeItemByGtin(gtin));
        } catch (Exception e) {
            logger.error("Error fetching trade item by GTIN {}: {}", gtin, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/trade-items/supplier/{supplierId}")
    public ResponseEntity<?> getTradeItemsBySupplierId(@PathVariable String supplierId) {
        try {
            return ResponseEntity.ok(tradeItemServiceClient.getTradeItemsBySupplierId(supplierId));
        } catch (Exception e) {
            logger.error("Error fetching trade items by supplier {}: {}", supplierId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Shipments ─────────────────────────────────────────────────────────────

    @GetMapping("/shipments")
    public ResponseEntity<?> getAllShipments() {
        try {
            return ResponseEntity.ok(shipmentServiceClient.getAllShipments());
        } catch (Exception e) {
            logger.error("Error fetching shipments: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipments/{id}")
    public ResponseEntity<?> getShipmentById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(shipmentServiceClient.getShipmentById(id));
        } catch (Exception e) {
            logger.error("Error fetching shipment {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipments/tracking/{trackingNumber}")
    public ResponseEntity<?> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        try {
            return ResponseEntity.ok(shipmentServiceClient.getShipmentByTrackingNumber(trackingNumber));
        } catch (Exception e) {
            logger.error("Error fetching shipment by tracking {}: {}", trackingNumber, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipments/status/{status}")
    public ResponseEntity<?> getShipmentsByStatus(@PathVariable String status) {
        try {
            return ResponseEntity.ok(shipmentServiceClient.getShipmentsByStatus(status));
        } catch (Exception e) {
            logger.error("Error fetching shipments by status {}: {}", status, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shipments/order/{orderId}")
    public ResponseEntity<?> getShipmentsByOrderId(@PathVariable String orderId) {
        try {
            return ResponseEntity.ok(shipmentServiceClient.getShipmentsByOrderId(orderId));
        } catch (Exception e) {
            logger.error("Error fetching shipments by order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Supplier Supply ───────────────────────────────────────────────────────

    @GetMapping("/supplier-supply")
    public ResponseEntity<?> getAllSupplierSupply() {
        try {
            return ResponseEntity.ok(supplierSupplyServiceClient.getAllSupplierSupply());
        } catch (Exception e) {
            logger.error("Error fetching supplier supply: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/supplier-supply/{id}")
    public ResponseEntity<?> getSupplierSupplyById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(supplierSupplyServiceClient.getSupplierSupplyById(id));
        } catch (Exception e) {
            logger.error("Error fetching supplier supply {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/supplier-supply/sku/{skuId}")
    public ResponseEntity<?> getSupplierSupplyBySkuId(@PathVariable String skuId) {
        try {
            return ResponseEntity.ok(supplierSupplyServiceClient.getSupplierSupplyBySkuId(skuId));
        } catch (Exception e) {
            logger.error("Error fetching supplier supply by SKU {}: {}", skuId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/supplier-supply/warehouse/{warehouseId}")
    public ResponseEntity<?> getSupplierSupplyByWarehouse(@PathVariable String warehouseId) {
        try {
            return ResponseEntity.ok(supplierSupplyServiceClient.getSupplierSupplyByWarehouse(warehouseId));
        } catch (Exception e) {
            logger.error("Error fetching supplier supply by warehouse {}: {}", warehouseId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
