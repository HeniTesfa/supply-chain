package com.supplychain.suppliersupply.service;

import com.supplychain.suppliersupply.entity.SupplierSupplyEntity;
import com.supplychain.suppliersupply.repository.SupplierSupplyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SupplierSupplyProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierSupplyProcessingService.class);

    @Autowired
    private SupplierSupplyRepository supplierSupplyRepository;

    public void processSupplierSupply(Map<String, Object> supplierSupplyEvent) {
        String skuId = (String) supplierSupplyEvent.get("skuId");
        String warehouseId = (String) supplierSupplyEvent.get("warehouseId");

        logger.info("Processing supplier supply - SKU: {}, Warehouse: {}", skuId, warehouseId);

        validateSupplierSupply(supplierSupplyEvent);
        saveSupplierSupply(supplierSupplyEvent);
        checkReorderPoint(skuId, warehouseId,
                getIntValue(supplierSupplyEvent, "availableQuantity"),
                getIntValue(supplierSupplyEvent, "reorderPoint"));

        logger.info("Supplier supply processing completed successfully");
    }

    private void saveSupplierSupply(Map<String, Object> event) {
        String skuId = (String) event.get("skuId");
        String warehouseId = (String) event.get("warehouseId");

        SupplierSupplyEntity entity = supplierSupplyRepository
                .findBySkuId(skuId).stream()
                .filter(e -> warehouseId.equals(e.getWarehouseId()))
                .findFirst()
                .orElse(SupplierSupplyEntity.builder()
                        .skuId(skuId)
                        .warehouseId(warehouseId)
                        .createdAt(LocalDateTime.now())
                        .build());

        entity.setAvailableQuantity(getIntValue(event, "availableQuantity"));
        entity.setReservedQuantity(getIntValue(event, "reservedQuantity"));
        entity.setOnOrderQuantity(getIntValue(event, "onOrderQuantity"));
        entity.setReorderPoint(getIntValue(event, "reorderPoint"));
        entity.setStatus((String) event.getOrDefault("status", "ACTIVE"));
        entity.setUpdatedAt(LocalDateTime.now());

        supplierSupplyRepository.save(entity);
        logger.info("Supplier supply saved to MongoDB - SKU: {}, Warehouse: {}", skuId, warehouseId);
    }

    private void validateSupplierSupply(Map<String, Object> event) {
        if (!event.containsKey("skuId") || event.get("skuId") == null) {
            throw new IllegalArgumentException("SKU ID is required");
        }
        if (((String) event.get("skuId")).trim().isEmpty()) {
            throw new IllegalArgumentException("SKU ID cannot be empty");
        }
        if (!event.containsKey("warehouseId") || event.get("warehouseId") == null) {
            throw new IllegalArgumentException("Warehouse ID is required");
        }
        if (((String) event.get("warehouseId")).trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse ID cannot be empty");
        }
        Integer availableQty = getIntValue(event, "availableQuantity");
        if (availableQty != null && availableQty < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative: " + availableQty);
        }
        Integer reservedQty = getIntValue(event, "reservedQuantity");
        if (reservedQty != null && reservedQty < 0) {
            throw new IllegalArgumentException("Reserved quantity cannot be negative: " + reservedQty);
        }
    }

    private void checkReorderPoint(String skuId, String warehouseId, Integer availableQty, Integer reorderPoint) {
        if (availableQty != null && reorderPoint != null && availableQty <= reorderPoint) {
            logger.warn("LOW STOCK ALERT - SKU: {}, Warehouse: {}, Available: {}, Reorder Point: {}",
                    skuId, warehouseId, availableQty, reorderPoint);
            if (availableQty == 0) {
                logger.error("OUT OF STOCK - SKU: {}, Warehouse: {}", skuId, warehouseId);
            }
        }
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
