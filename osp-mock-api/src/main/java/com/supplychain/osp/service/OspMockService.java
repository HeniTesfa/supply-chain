// Package declaration: places this class in the OSP mock API service layer of the supply chain application
package com.supplychain.osp.service;

// Import the SLF4J Logger interface for creating structured log statements throughout the service
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory, a factory class used to create Logger instances bound to specific classes
import org.slf4j.LoggerFactory;
// Import the Spring @Service annotation to mark this class as a Spring-managed service component bean
import org.springframework.stereotype.Service;

// Import the LocalDateTime class for generating timestamps when items are created or updated
import java.time.LocalDateTime;
// Import multiple Java utility classes: ArrayList, Arrays, HashMap, List, Map for data structure operations
import java.util.*;
// Import ConcurrentHashMap, a thread-safe hash map implementation used as the in-memory data store
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSP Mock Service
 *
 * Simulates external OSP (Order Service Provider) API behavior
 * Maintains in-memory storage of items for testing
 */
// Marks this class as a Spring service component, registering it as a singleton bean in the application context
// Spring's component scanning will automatically detect and instantiate this class at startup
@Service
// Declares the OspMockService class that simulates an external OSP API with in-memory item storage
public class OspMockService {

    // Creates a static final Logger instance bound to this class for logging all mock API operations
    private static final Logger logger = LoggerFactory.getLogger(OspMockService.class);

    // In-memory storage for mock data
    // Declares a ConcurrentHashMap to store item data keyed by SKU ID, providing thread-safe concurrent access
    // The outer Map maps SKU ID strings to inner Maps that represent the item's properties as key-value pairs
    private final Map<String, Map<String, Object>> itemStorage = new ConcurrentHashMap<>();

    // Constructor for OspMockService, called by Spring during bean creation
    // Initializes the in-memory store with pre-populated test data for immediate availability
    public OspMockService() {
        // Pre-populate with some test data
        // Calls the initializeTestData() method to seed the in-memory storage with sample items
        initializeTestData();
    }

    /**
     * Get item data by SKU ID
     */
    // Public method that retrieves item data from the in-memory store by SKU ID
    // Returns the item's Map<String, Object> data if found, or throws a RuntimeException if not found
    public Map<String, Object> getItemData(String skuId) {
        // Logs an info message indicating an incoming GET request for the specified SKU ID
        logger.info("OSP API: GET request for SKU: {}", skuId);

        // Checks if the in-memory item storage contains an entry for the requested SKU ID
        if (itemStorage.containsKey(skuId)) {
            // Retrieves the item data map from storage using the SKU ID as the key
            Map<String, Object> itemData = itemStorage.get(skuId);
            // Logs the item name from the retrieved data to confirm the item was found
            logger.info("OSP API: Item found - {}", itemData.get("itemName"));
            // Returns the complete item data map to the caller
            return itemData;
        // If the SKU ID does not exist in the storage, the item was not found
        } else {
            // Logs a warning message indicating that no item exists for the requested SKU ID
            logger.warn("OSP API: Item not found for SKU: {}", skuId);
            // Throws a RuntimeException to simulate a 404-like "not found" response from the external OSP API
            throw new RuntimeException("Item not found");
        }
    }

    /**
     * Create or update item
     */
    // Public method that creates a new item or updates an existing item in the in-memory store
    // Accepts item data as a Map, adds metadata, stores it, and returns a success response map
    public Map<String, Object> createOrUpdateItem(Map<String, Object> itemData) {
        // Extracts the SKU ID from the incoming item data map, which serves as the unique identifier
        String skuId = (String) itemData.get("skuId");
        // Logs an info message indicating an incoming POST request to create or update the specified SKU
        logger.info("OSP API: POST request to create/update SKU: {}", skuId);

        // Validates that the SKU ID is present and not empty; it is required for item identification
        if (skuId == null || skuId.trim().isEmpty()) {
            // Throws an IllegalArgumentException if the SKU ID is missing or blank
            throw new IllegalArgumentException("SKU ID is required");
        }

        // Add metadata
        // Adds a "lastUpdated" timestamp field to the item data, recording when this create/update occurred
        itemData.put("lastUpdated", LocalDateTime.now().toString());
        // Adds an "ospApiVersion" field to the item data to track which version of the mock API processed it
        itemData.put("ospApiVersion", "1.0");

        // Store item
        // Puts the item data into the ConcurrentHashMap, keyed by SKU ID; overwrites any existing entry for the same SKU
        itemStorage.put(skuId, itemData);

        // Logs an info message confirming the item was successfully created or updated in the store
        logger.info("OSP API: Item created/updated successfully - SKU: {}", skuId);

        // Returns an immutable response map containing a success flag, the SKU ID, a confirmation message, and a timestamp
        // Map.of() creates an unmodifiable map with four key-value pairs representing the API response
        return Map.of(
            "success", true,
            "skuId", skuId,
            "message", "Item processed successfully by OSP API",
            "timestamp", LocalDateTime.now().toString()
        );
    }

    /**
     * Initialize test data
     */
    // Private method that pre-populates the in-memory storage with sample items for testing and development
    // Called once during construction to ensure test data is available immediately when the service starts
    private void initializeTestData() {
        // Add some sample items for testing
        // Creates a list of five test item maps using the createTestItem() helper method with sample product data
        // Each item represents a different electronics product with unique SKU, name, category, price, and weight
        List<Map<String, Object>> testItems = Arrays.asList(
            // Creates a test item: SKU001 - Laptop in Electronics category, priced at $999.99, weighing 2.5 kg
            createTestItem("SKU001", "Laptop", "Electronics", 999.99, 2.5),
            // Creates a test item: SKU002 - Mouse in Electronics category, priced at $29.99, weighing 0.2 kg
            createTestItem("SKU002", "Mouse", "Electronics", 29.99, 0.2),
            // Creates a test item: SKU003 - Keyboard in Electronics category, priced at $79.99, weighing 0.8 kg
            createTestItem("SKU003", "Keyboard", "Electronics", 79.99, 0.8),
            // Creates a test item: SKU004 - Monitor in Electronics category, priced at $299.99, weighing 5.0 kg
            createTestItem("SKU004", "Monitor", "Electronics", 299.99, 5.0),
            // Creates a test item: SKU005 - Headphones in Electronics category, priced at $149.99, weighing 0.3 kg
            createTestItem("SKU005", "Headphones", "Electronics", 149.99, 0.3)
        );

        // Iterates over each test item in the list to add them to the in-memory storage
        for (Map<String, Object> item : testItems) {
            // Extracts the SKU ID from the current test item to use as the storage map key
            String skuId = (String) item.get("skuId");
            // Stores the test item in the ConcurrentHashMap, keyed by its SKU ID for later retrieval
            itemStorage.put(skuId, item);
        }

        // Logs a success message confirming how many test items were loaded into the in-memory storage
        logger.info("✅ OSP Mock API initialized with {} test items", testItems.size());
    }

    /**
     * Create test item
     */
    // Private helper method that constructs a Map representing a single test item with the given properties
    // Returns a mutable HashMap with standard item fields populated for use in test data initialization
    private Map<String, Object> createTestItem(String skuId, String itemName,
                                                String category, Double price, Double weight) {
        // Creates a new mutable HashMap to hold the item's key-value property pairs
        Map<String, Object> item = new HashMap<>();
        // Adds the SKU ID to the item map as the unique product identifier
        item.put("skuId", skuId);
        // Adds the item name to the map (e.g., "Laptop", "Mouse") for display and search purposes
        item.put("itemName", itemName);
        // Adds the product category to the map (e.g., "Electronics") for classification and filtering
        item.put("category", category);
        // Adds the price to the map as a Double value representing the item's cost in dollars
        item.put("price", price);
        // Adds the weight to the map as a Double value representing the item's weight in kilograms
        item.put("weight", weight);
        // Adds default dimensions as a String; all test items share the same placeholder dimensions
        item.put("dimensions", "10x10x10 cm");
        // Adds the status field set to "ACTIVE", indicating the item is available and not discontinued
        item.put("status", "ACTIVE");
        // Adds a "lastUpdated" timestamp recording when this test item was created
        item.put("lastUpdated", LocalDateTime.now().toString());
        // Adds the OSP API version metadata to identify which version of the mock API created this item
        item.put("ospApiVersion", "1.0");
        // Returns the fully populated item map to the caller
        return item;
    }

    /**
     * Get all items (for debugging)
     */
    // Public method that returns all items currently stored in the in-memory storage
    // Returns a new ArrayList containing all item maps from the storage, useful for debugging and admin endpoints
    public List<Map<String, Object>> getAllItems() {
        // Creates and returns a new ArrayList from the values collection of the itemStorage ConcurrentHashMap
        // This returns a snapshot of all items; modifications to the returned list do not affect the storage
        return new ArrayList<>(itemStorage.values());
    }

    /**
     * Clear all items (for testing)
     */
    // Public method that removes all items from the in-memory storage, useful for resetting state between tests
    public void clearAllItems() {
        // Removes all entries from the ConcurrentHashMap, leaving the storage completely empty
        itemStorage.clear();
        // Logs an info message confirming that all items have been removed from the store
        logger.info("OSP API: All items cleared");
    }

    /**
     * Get item count
     */
    // Public method that returns the current number of items stored in the in-memory storage
    // Used by the health check endpoint to report storage utilization
    public int getItemCount() {
        // Returns the number of key-value pairs currently in the ConcurrentHashMap
        return itemStorage.size();
    }
}
