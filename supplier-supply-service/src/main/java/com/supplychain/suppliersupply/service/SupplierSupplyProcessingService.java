// Package declaration: places this class in the supplier supply service layer of the supply chain application
package com.supplychain.suppliersupply.service;

// Import the SLF4J Logger interface for creating structured log statements throughout the service
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory, a factory class used to create Logger instances bound to specific classes
import org.slf4j.LoggerFactory;
// Import the Spring @Service annotation to mark this class as a Spring-managed service component bean
import org.springframework.stereotype.Service;

// Import the Java Map interface for handling key-value pair event data received from the loader service
import java.util.Map;

/**
 * Supplier Supply Processing Service
 *
 * Core business logic for processing supplier supply events:
 * - Available quantity updates
 * - Reserved quantity tracking
 * - On-order quantity management
 * - Reorder point monitoring
 * - Low stock alerts
 * - Warehouse-specific supply levels
 */
// Marks this class as a Spring service component, registering it as a singleton bean in the application context
// Spring's component scanning will automatically detect and instantiate this class at startup
@Service
// Declares the SupplierSupplyProcessingService class containing all supplier supply event processing business logic
public class SupplierSupplyProcessingService {

    // Creates a static final Logger instance bound to this class for consistent, class-identified log output
    // The logger is used throughout the service to track supply processing steps, alerts, and errors
    private static final Logger logger = LoggerFactory.getLogger(SupplierSupplyProcessingService.class);

    /**
     * Process supplier supply event
     *
     * @param supplierSupplyEvent - Supplier supply event data from loader service
     */
    // Public method serving as the main entry point for processing a supplier supply event from the loader service
    // Accepts a Map<String, Object> representing the deserialized JSON event payload
    public void processSupplierSupply(Map<String, Object> supplierSupplyEvent) {
        // Extracts the SKU identifier from the event map, used to identify which product this supply data belongs to
        String skuId = (String) supplierSupplyEvent.get("skuId");
        // Extracts the warehouse identifier from the event map, indicating which warehouse holds this supply
        String warehouseId = (String) supplierSupplyEvent.get("warehouseId");
        // Extracts and converts the available quantity using the getIntValue() helper for safe type conversion
        Integer availableQty = getIntValue(supplierSupplyEvent, "availableQuantity");
        // Extracts and converts the reserved quantity (stock allocated to existing orders but not yet shipped)
        Integer reservedQty = getIntValue(supplierSupplyEvent, "reservedQuantity");
        // Extracts and converts the reorder point threshold that triggers low stock alerts when available stock falls to or below it
        Integer reorderPoint = getIntValue(supplierSupplyEvent, "reorderPoint");

        // Logs an informational message with key supply data fields for operational visibility and troubleshooting
        logger.info("Processing supplier supply - SKU: {}, Warehouse: {}, Available: {}, Reserved: {}",
            skuId, warehouseId, availableQty, reservedQty);

        // Step 1: Calls the validation method to ensure all required fields are present and quantities are non-negative
        validateSupplierSupply(supplierSupplyEvent);

        // Step 2: Calls the reorder point check method to detect low stock conditions and generate alerts
        checkReorderPoint(skuId, warehouseId, availableQty, reorderPoint);

        // Step 3: Calls the total supply calculation method to compute on-hand and future supply totals
        calculateTotalSupply(supplierSupplyEvent);

        // In a real system, you would:
        // 1. Update supply database/WMS
        // 2. Sync with ecommerce platform
        // 3. Trigger replenishment if needed
        // 4. Update supply visibility
        // 5. Notify warehouse management

        // Logs a success message indicating the entire supplier supply processing pipeline completed without errors
        logger.info("Supplier supply processing completed successfully");
    }

    /**
     * Validate supplier supply data
     */
    // Private method that validates all required and optional fields in the supplier supply event payload
    // Throws IllegalArgumentException if any validation rule is violated, halting further processing
    private void validateSupplierSupply(Map<String, Object> supplierSupplyEvent) {
        // Logs a debug-level message indicating the start of the validation phase
        logger.debug("Validating supplier supply data...");

        // Required: SKU ID
        // Checks if the event map contains the "skuId" key; absence means a required field is missing
        if (!supplierSupplyEvent.containsKey("skuId")) {
            // Throws an IllegalArgumentException to reject events that lack the SKU ID field entirely
            throw new IllegalArgumentException("SKU ID is required");
        }

        // Retrieves the SKU ID value from the map, casting it to String for further validation
        String skuId = (String) supplierSupplyEvent.get("skuId");
        // Checks if the SKU ID is null or contains only whitespace, which are invalid even when the key exists
        if (skuId == null || skuId.trim().isEmpty()) {
            // Throws an IllegalArgumentException because a present but empty SKU ID is not acceptable
            throw new IllegalArgumentException("SKU ID cannot be empty");
        }

        // Required: Warehouse ID
        // Checks if the event map contains the "warehouseId" key, which is mandatory for warehouse-level tracking
        if (!supplierSupplyEvent.containsKey("warehouseId")) {
            // Throws an IllegalArgumentException to reject events that do not specify a warehouse
            throw new IllegalArgumentException("Warehouse ID is required");
        }

        // Retrieves the warehouse ID value from the map, casting it to String for validation
        String warehouseId = (String) supplierSupplyEvent.get("warehouseId");
        // Checks if the warehouse ID is null or empty, which would prevent proper warehouse-level supply tracking
        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            // Throws an IllegalArgumentException because warehouse ID must be a non-empty value
            throw new IllegalArgumentException("Warehouse ID cannot be empty");
        }

        // Validate quantities are non-negative
        // Extracts and converts the available quantity for non-negative validation
        Integer availableQty = getIntValue(supplierSupplyEvent, "availableQuantity");
        // Checks if available quantity is present and negative, which is physically impossible for stock levels
        if (availableQty != null && availableQty < 0) {
            // Throws an IllegalArgumentException with the offending value included in the error message
            throw new IllegalArgumentException(
                "Available quantity cannot be negative: " + availableQty
            );
        }

        // Extracts and converts the reserved quantity for non-negative validation
        Integer reservedQty = getIntValue(supplierSupplyEvent, "reservedQuantity");
        // Checks if reserved quantity is present and negative, which is an invalid state for reserved stock
        if (reservedQty != null && reservedQty < 0) {
            // Throws an IllegalArgumentException indicating that reserved quantities must be zero or positive
            throw new IllegalArgumentException(
                "Reserved quantity cannot be negative: " + reservedQty
            );
        }

        // Extracts and converts the on-order quantity for non-negative validation
        Integer onOrderQty = getIntValue(supplierSupplyEvent, "onOrderQuantity");
        // Checks if on-order quantity is present and negative, which would be an invalid procurement state
        if (onOrderQty != null && onOrderQty < 0) {
            // Throws an IllegalArgumentException indicating that on-order quantities cannot be negative
            throw new IllegalArgumentException(
                "On-order quantity cannot be negative: " + onOrderQty
            );
        }

        // Logs a debug-level confirmation that all validation checks passed, including the SKU and warehouse identifiers
        logger.debug("Supplier supply validation passed - SKU: {}, Warehouse: {}",
            skuId, warehouseId);
    }

    /**
     * Check reorder point and generate alerts
     */
    // Private method that compares available quantity against the reorder point threshold
    // Generates LOW STOCK ALERT or OUT OF STOCK warnings when supply drops to or below the reorder point
    private void checkReorderPoint(String skuId, String warehouseId,
                                   Integer availableQty, Integer reorderPoint) {

        // Checks if both available quantity and reorder point are provided AND if available stock is at or below the reorder threshold
        if (availableQty != null && reorderPoint != null && availableQty <= reorderPoint) {
            // Logs a warning-level LOW STOCK ALERT with all relevant details for supply chain operations to act upon
            logger.warn("LOW STOCK ALERT - SKU: {}, Warehouse: {}, Available: {}, Reorder Point: {}",
                skuId, warehouseId, availableQty, reorderPoint);

            // In a real system:
            // 1. Create purchase order automatically
            // 2. Send alert to procurement team
            // 3. Notify warehouse manager
            // 4. Update planning system
            // 5. Flag for urgent replenishment

            // Checks specifically if available quantity has reached zero, indicating a complete stockout
            if (availableQty == 0) {
                // Logs an error-level OUT OF STOCK message, the most critical supply alert requiring immediate action
                logger.error("OUT OF STOCK - SKU: {}, Warehouse: {}", skuId, warehouseId);
            }
        // If both values are present but available quantity exceeds the reorder point, stock levels are healthy
        } else if (availableQty != null && reorderPoint != null) {
            // Logs a debug-level message confirming that stock levels are above the reorder threshold
            logger.debug("Stock levels healthy - Available: {} > Reorder point: {}",
                availableQty, reorderPoint);
        }
    }

    /**
     * Calculate total supply across all states
     */
    // Private method that calculates total on-hand supply and total future supply including on-order quantities
    // Provides a comprehensive view of current and projected supply levels for the given event
    private void calculateTotalSupply(Map<String, Object> supplierSupplyEvent) {
        // Extracts the available quantity (stock ready to sell) using the safe type conversion helper
        Integer availableQty = getIntValue(supplierSupplyEvent, "availableQuantity");
        // Extracts the reserved quantity (stock allocated to orders) using the safe type conversion helper
        Integer reservedQty = getIntValue(supplierSupplyEvent, "reservedQuantity");
        // Extracts the on-order quantity (stock ordered from suppliers but not yet received) using the safe type conversion helper
        Integer onOrderQty = getIntValue(supplierSupplyEvent, "onOrderQuantity");

        // Calculates total on-hand supply by summing available and reserved quantities, defaulting nulls to zero
        // On-hand represents all physical stock currently in the warehouse regardless of allocation status
        int totalOnHand = (availableQty != null ? availableQty : 0) +
                          (reservedQty != null ? reservedQty : 0);

        // Calculates total future supply by adding on-order quantities to the on-hand total, defaulting null to zero
        // Future supply represents the projected stock level once all pending orders from suppliers are received
        int totalFuture = totalOnHand + (onOrderQty != null ? onOrderQty : 0);

        // Logs the supply summary at info level, showing both the current on-hand and projected future supply totals
        logger.info("Supply summary - On-hand: {}, Future (incl. on-order): {}",
            totalOnHand, totalFuture);

        // In a real system:
        // 1. Update supply dashboard
        // 2. Sync with ATP (Available-to-Promise) calculation
        // 3. Update sales channel supply levels
        // 4. Calculate safety stock coverage
    }

    /**
     * Helper method to get integer value
     */
    // Private utility method that safely extracts and converts a value from a map to an Integer
    // Handles three cases: null values, Number instances (Integer, Long, Double), and String representations
    private Integer getIntValue(Map<String, Object> map, String key) {
        // Retrieves the raw Object value from the map using the specified key
        Object value = map.get(key);
        // Returns null immediately if the value is not present, indicating the field was not provided in the event
        if (value == null) return null;
        // If the value is already a Number instance (e.g., Integer from JSON deserialization), converts to int and wraps as Integer
        if (value instanceof Number) return ((Number) value).intValue();
        // As a fallback, treats the value as a String and parses it to Integer (may throw NumberFormatException if not numeric)
        return Integer.parseInt(value.toString());
    }
}
