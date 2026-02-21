package com.supplychain.tradeitem.service;

import com.supplychain.tradeitem.entity.TradeItemEntity;
import com.supplychain.tradeitem.repository.TradeItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TradeItemProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(TradeItemProcessingService.class);

    @Autowired
    private TradeItemRepository tradeItemRepository;

    public void processTradeItem(Map<String, Object> tradeItemEvent) {
        String gtin = (String) tradeItemEvent.get("gtin");
        String skuId = (String) tradeItemEvent.get("skuId");
        String supplierName = (String) tradeItemEvent.get("supplierName");

        logger.info("Processing trade item - GTIN: {}, SKU: {}, Supplier: {}", gtin, skuId, supplierName);

        validateTradeItem(tradeItemEvent);
        saveTradeItem(tradeItemEvent);

        logger.info("Trade item processing completed successfully");
    }

    private void saveTradeItem(Map<String, Object> tradeItemEvent) {
        String gtin = (String) tradeItemEvent.get("gtin");

        TradeItemEntity entity = tradeItemRepository.findByGtin(gtin)
                .orElse(TradeItemEntity.builder()
                        .gtin(gtin)
                        .createdAt(LocalDateTime.now())
                        .build());

        entity.setSkuId((String) tradeItemEvent.get("skuId"));
        entity.setSupplierId((String) tradeItemEvent.get("supplierId"));
        entity.setSupplierName((String) tradeItemEvent.get("supplierName"));
        entity.setUnitOfMeasure((String) tradeItemEvent.get("unitOfMeasure"));
        entity.setStatus((String) tradeItemEvent.getOrDefault("status", "ACTIVE"));
        entity.setMinOrderQuantity(getIntValue(tradeItemEvent, "minOrderQuantity"));
        entity.setLeadTimeDays(getIntValue(tradeItemEvent, "leadTimeDays"));
        entity.setUpdatedAt(LocalDateTime.now());

        tradeItemRepository.save(entity);
        logger.info("Trade item saved to MongoDB - GTIN: {}", gtin);
    }

    private void validateTradeItem(Map<String, Object> tradeItemEvent) {
        if (!tradeItemEvent.containsKey("gtin")) {
            throw new IllegalArgumentException("GTIN is required for trade items");
        }
        String gtin = (String) tradeItemEvent.get("gtin");
        if (gtin == null || gtin.trim().isEmpty()) {
            throw new IllegalArgumentException("GTIN cannot be empty");
        }
        if (!gtin.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
            throw new IllegalArgumentException("GTIN must be 8, 12, 13, or 14 digits. Received: " + gtin);
        }
        if (!tradeItemEvent.containsKey("skuId")) {
            throw new IllegalArgumentException("SKU ID is required");
        }
        Integer minQty = getIntValue(tradeItemEvent, "minOrderQuantity");
        if (minQty != null && minQty < 1) {
            throw new IllegalArgumentException("Min order quantity must be at least 1");
        }
        Integer leadTime = getIntValue(tradeItemEvent, "leadTimeDays");
        if (leadTime != null && leadTime < 0) {
            throw new IllegalArgumentException("Lead time cannot be negative");
        }
    }

    private void processSupplierInfo(Map<String, Object> tradeItemEvent) {
        String supplierId = (String) tradeItemEvent.get("supplierId");
        String supplierName = (String) tradeItemEvent.get("supplierName");
        if (supplierId != null || supplierName != null) {
            logger.info("Supplier info - ID: {}, Name: {}", supplierId, supplierName);
        }
    }

    private void processOrderingInfo(Map<String, Object> tradeItemEvent) {
        Integer minOrderQty = getIntValue(tradeItemEvent, "minOrderQuantity");
        Integer leadTimeDays = getIntValue(tradeItemEvent, "leadTimeDays");
        String unitOfMeasure = (String) tradeItemEvent.get("unitOfMeasure");
        if (minOrderQty != null || leadTimeDays != null || unitOfMeasure != null) {
            logger.info("Ordering info - Min qty: {}, Lead time: {} days, UOM: {}", minOrderQty, leadTimeDays, unitOfMeasure);
            if (leadTimeDays != null && leadTimeDays > 30) {
                logger.warn("Long lead time detected: {} days", leadTimeDays);
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
