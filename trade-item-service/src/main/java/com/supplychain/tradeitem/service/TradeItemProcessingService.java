// Package declaration: places this class in the trade item service layer of the supply chain application
package com.supplychain.tradeitem.service;

// Import the SLF4J Logger interface for creating log statements throughout the service
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory, a factory class used to create Logger instances
import org.slf4j.LoggerFactory;
// Import the Spring @Service annotation to mark this class as a Spring-managed service bean
import org.springframework.stereotype.Service;

// Import the Java Map interface for handling key-value pair event data passed as method parameters
import java.util.Map;

/**
 * Trade Item Processing Service
 *
 * Core business logic for processing trade item events:
 * - GTIN (Global Trade Item Number) validation
 * - Supplier information management
 * - Product descriptions
 * - Unit of measure
 * - Min order quantities
 * - Lead times
 */
// Marks this class as a Spring service component, making it eligible for auto-detection via classpath scanning
// Spring will create a singleton bean of this class and manage its lifecycle in the application context
@Service
// Declares the TradeItemProcessingService class which contains all trade item event processing business logic
public class TradeItemProcessingService {

    // Creates a static final Logger instance bound to this class for structured logging throughout the service
    // LoggerFactory.getLogger() accepts the class literal to automatically include the class name in log output
    private static final Logger logger = LoggerFactory.getLogger(TradeItemProcessingService.class);

    /**
     * Process trade item event
     *
     * @param tradeItemEvent - Trade item event data from loader service
     */
    // Public method that serves as the main entry point for processing a trade item event
    // Accepts a Map<String, Object> representing the deserialized JSON event payload from the loader service
    public void processTradeItem(Map<String, Object> tradeItemEvent) {
        // Extracts the GTIN (Global Trade Item Number) from the event map by casting the value to String
        String gtin = (String) tradeItemEvent.get("gtin");
        // Extracts the SKU identifier from the event map, used to uniquely identify the product internally
        String skuId = (String) tradeItemEvent.get("skuId");
        // Extracts the supplier name from the event map for logging and downstream processing
        String supplierName = (String) tradeItemEvent.get("supplierName");

        // Logs an informational message indicating that trade item processing has started, including key identifiers
        logger.info("🔄 Processing trade item - GTIN: {}, SKU: {}, Supplier: {}",
            gtin, skuId, supplierName);

        // Step 1: Calls the validation method to ensure all required fields are present and correctly formatted
        validateTradeItem(tradeItemEvent);

        // Step 2: Calls the supplier information processing method to handle supplier-related data
        processSupplierInfo(tradeItemEvent);

        // Step 3: Calls the ordering information processing method to handle min order quantities, lead times, and UOM
        processOrderingInfo(tradeItemEvent);

        // In a real system, you would:
        // 1. Update product catalog database
        // 2. Sync with ERP system
        // 3. Update supplier portal
        // 4. Trigger pricing updates
        // 5. Update procurement system

        // Logs a success message indicating the entire trade item processing pipeline completed without errors
        logger.info("✅ Trade item processing completed successfully");
    }

    /**
     * Validate trade item data
     */
    // Private method that validates all required and optional fields in the trade item event payload
    // Throws IllegalArgumentException if any validation rule is violated, halting further processing
    private void validateTradeItem(Map<String, Object> tradeItemEvent) {
        // Logs a debug-level message indicating the start of the validation phase
        logger.debug("Validating trade item data...");

        // Required: GTIN
        // Checks if the event map contains the "gtin" key at all; absence means the required field is missing
        if (!tradeItemEvent.containsKey("gtin")) {
            // Throws an IllegalArgumentException to reject events that lack a GTIN field entirely
            throw new IllegalArgumentException("GTIN is required for trade items");
        }

        // Retrieves the GTIN value from the map, casting it to String for format validation
        String gtin = (String) tradeItemEvent.get("gtin");
        // Checks if the GTIN is null or contains only whitespace, which are invalid even though the key exists
        if (gtin == null || gtin.trim().isEmpty()) {
            // Throws an IllegalArgumentException because a present but empty GTIN is not acceptable
            throw new IllegalArgumentException("GTIN cannot be empty");
        }

        // Validate GTIN format (8, 12, 13, or 14 digits)
        // Uses a regex pattern to ensure the GTIN matches one of the four valid lengths: GTIN-8, GTIN-12, GTIN-13, or GTIN-14
        if (!gtin.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
            // Throws an IllegalArgumentException with a descriptive message including the invalid GTIN value received
            throw new IllegalArgumentException(
                "GTIN must be 8, 12, 13, or 14 digits. Received: " + gtin
            );
        }

        // Required: SKU ID
        // Checks if the event map contains the "skuId" key, which is mandatory for identifying the product
        if (!tradeItemEvent.containsKey("skuId")) {
            // Throws an IllegalArgumentException if the SKU ID field is missing from the event
            throw new IllegalArgumentException("SKU ID is required");
        }

        // Validate min order quantity (if present)
        // Checks if the optional "minOrderQuantity" field exists in the event before attempting validation
        if (tradeItemEvent.containsKey("minOrderQuantity")) {
            // Retrieves the minOrderQuantity value as a raw Object since it could be a Number or String type
            Object minQty = tradeItemEvent.get("minOrderQuantity");
            // Only validates the value if it is not null; null is acceptable for optional fields
            if (minQty != null) {
                // Converts the value to int using a ternary: if it is a Number instance, use intValue();
                // otherwise parse the string representation as an integer
                int quantity = minQty instanceof Number ?
                    ((Number) minQty).intValue() :
                    Integer.parseInt(minQty.toString());

                // Validates that the minimum order quantity is at least 1 (cannot order zero or negative items)
                if (quantity < 1) {
                    // Throws an IllegalArgumentException if the minimum order quantity is less than 1
                    throw new IllegalArgumentException(
                        "Min order quantity must be at least 1"
                    );
                }
            }
        }

        // Validate lead time (if present)
        // Checks if the optional "leadTimeDays" field exists in the event before attempting validation
        if (tradeItemEvent.containsKey("leadTimeDays")) {
            // Retrieves the leadTimeDays value as a raw Object to handle both Number and String representations
            Object leadTime = tradeItemEvent.get("leadTimeDays");
            // Only validates the value if it is not null; null values are allowed for optional fields
            if (leadTime != null) {
                // Converts the value to int: uses intValue() if it is a Number instance, otherwise parses the string
                int days = leadTime instanceof Number ?
                    ((Number) leadTime).intValue() :
                    Integer.parseInt(leadTime.toString());

                // Validates that lead time in days is not negative (zero is acceptable, meaning same-day)
                if (days < 0) {
                    // Throws an IllegalArgumentException if lead time is a negative number
                    throw new IllegalArgumentException(
                        "Lead time cannot be negative"
                    );
                }
            }
        }

        // Logs a debug-level confirmation that all validation checks passed, including the validated GTIN
        logger.debug("✓ Trade item validation passed - GTIN: {}", gtin);
    }

    /**
     * Process supplier information
     */
    // Private method that processes supplier-related fields from the trade item event
    // Extracts and logs supplier ID and name; in a real system would update supplier-item relationships
    private void processSupplierInfo(Map<String, Object> tradeItemEvent) {
        // Extracts the supplier ID from the event map, which uniquely identifies the supplier in the system
        String supplierId = (String) tradeItemEvent.get("supplierId");
        // Extracts the supplier name from the event map for display and logging purposes
        String supplierName = (String) tradeItemEvent.get("supplierName");

        // Only processes supplier info if at least one of supplierId or supplierName is provided (not null)
        if (supplierId != null || supplierName != null) {
            // Logs the supplier information at info level for operational visibility
            logger.info("📦 Supplier info - ID: {}, Name: {}", supplierId, supplierName);

            // In a real system:
            // 1. Validate supplier exists in supplier master
            // 2. Check supplier authorization/approval status
            // 3. Verify supplier can supply this GTIN
            // 4. Update supplier-item relationships
        }
    }

    /**
     * Process ordering information
     */
    // Private method that processes ordering-related fields: min order quantity, lead time, and unit of measure
    // Logs ordering parameters and generates a warning if lead time exceeds 30 days
    private void processOrderingInfo(Map<String, Object> tradeItemEvent) {
        // Extracts and converts the minimum order quantity using the getIntValue() helper for safe type conversion
        Integer minOrderQty = getIntValue(tradeItemEvent, "minOrderQuantity");
        // Extracts and converts the lead time in days using the getIntValue() helper for safe type conversion
        Integer leadTimeDays = getIntValue(tradeItemEvent, "leadTimeDays");
        // Extracts the unit of measure as a String (e.g., "EACH", "CASE", "PALLET")
        String unitOfMeasure = (String) tradeItemEvent.get("unitOfMeasure");

        // Only processes ordering info if at least one ordering parameter is present (not null)
        if (minOrderQty != null || leadTimeDays != null || unitOfMeasure != null) {
            // Logs the extracted ordering information at info level for operational tracking
            logger.info("📋 Ordering info - Min qty: {}, Lead time: {} days, UOM: {}",
                minOrderQty, leadTimeDays, unitOfMeasure);

            // In a real system:
            // 1. Update procurement parameters
            // 2. Calculate reorder points
            // 3. Update supply planning
            // 4. Notify purchasing team of changes

            // Checks if lead time exceeds 30 days, which is considered unusually long and worth flagging
            if (leadTimeDays != null && leadTimeDays > 30) {
                // Logs a warning-level message to alert operations that this item has an exceptionally long lead time
                logger.warn("⚠️  Long lead time detected: {} days", leadTimeDays);
            }
        }
    }

    /**
     * Helper method to get integer value
     */
    // Private utility method that safely extracts and converts a value from a map to an Integer
    // Handles three cases: null values, Number instances, and String representations of integers
    private Integer getIntValue(Map<String, Object> map, String key) {
        // Retrieves the raw Object value from the map using the specified key
        Object value = map.get(key);
        // Returns null immediately if the value is not present in the map, indicating the field was not provided
        if (value == null) return null;
        // If the value is already a Number instance (Integer, Long, Double, etc.), converts it to int and wraps as Integer
        if (value instanceof Number) return ((Number) value).intValue();
        // As a fallback, treats the value as a String and parses it to Integer (may throw NumberFormatException)
        return Integer.parseInt(value.toString());
    }
}
