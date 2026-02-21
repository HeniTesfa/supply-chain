package com.supplychain.consumer.service; // Define the package for the consumer service layer

// Jackson library import for JSON serialization/deserialization of event payloads
import com.fasterxml.jackson.databind.ObjectMapper;
// Import all entity classes (ItemEntity, TradeItemEntity, SupplierSupplyEntity, ShipmentEntity, ErrorLog, ProcessingStatus)
import com.supplychain.consumer.entity.*;
// Import all model/DTO classes (ItemEvent, TradeItemEvent, SupplierSupplyEvent, ShipmentEvent, SupplyChainEvent)
import com.supplychain.consumer.model.*;
// Import all Spring Data MongoDB repository interfaces for database operations
import com.supplychain.consumer.repository.*;
// SLF4J logging facade for structured log output
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Spring dependency injection annotation for auto-wiring beans
import org.springframework.beans.factory.annotation.Autowired;
// Spring Kafka annotation that marks this method as a Kafka message listener
import org.springframework.kafka.annotation.KafkaListener;
// Spring Kafka Acknowledgment interface for manual offset commits (Level 1 dedup)
import org.springframework.kafka.support.Acknowledgment;
// Spring Kafka header constants for extracting metadata like topic name from messages
import org.springframework.kafka.support.KafkaHeaders;
// Spring messaging annotation to extract specific headers from the incoming Kafka message
import org.springframework.messaging.handler.annotation.Header;
// Spring messaging annotation to extract the message payload (body) from the Kafka record
import org.springframework.messaging.handler.annotation.Payload;
// Spring stereotype annotation that marks this class as a service-layer component for DI
import org.springframework.stereotype.Service;

// Java standard charset for consistent UTF-8 encoding during hash computation
import java.nio.charset.StandardCharsets;
// Java security class for computing SHA-256 message digests (used in Level 4 dedup)
import java.security.MessageDigest;
// Java time class for timestamping entity creation and update events
import java.time.LocalDateTime;
// Java Map interface used as the generic event payload container (flexible schema)
import java.util.Map;

/**
 * Kafka Consumer Service
 *
 * Main service for consuming events from Kafka with 4-level deduplication:
 * Level 1: Kafka offset management (exactly-once) — manual acknowledgment ensures
 *          offsets are only committed after successful processing
 * Level 2: Event ID check (idempotency) — database lookup to detect previously processed events
 * Level 3: Business key check (SKU, GTIN, tracking number) — finds existing entities by
 *          their domain-specific unique identifier
 * Level 4: Content hash check (detect if data actually changed) — SHA-256 hash comparison
 *          prevents unnecessary updates when payload content is identical
 */
@Service // Registers this class as a Spring-managed service bean, enabling dependency injection
public class KafkaConsumerService {

    // Create a logger instance specific to this class for contextual log messages
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired // Inject the Item MongoDB repository for item entity CRUD operations
    private ItemRepository itemRepository;

    @Autowired // Inject the Trade Item MongoDB repository for trade item entity CRUD operations
    private TradeItemRepository tradeItemRepository;

    @Autowired // Inject the Supplier Supply MongoDB repository for supplier supply entity CRUD operations
    private SupplierSupplyRepository supplierSupplyRepository;

    @Autowired // Inject the Shipment MongoDB repository for shipment entity CRUD operations
    private ShipmentRepository shipmentRepository;

    @Autowired // Inject the Error Log MongoDB repository for persisting processing failures
    private ErrorLogRepository errorLogRepository;

    @Autowired // Inject the HTTP client that forwards processed events to the loader service
    private LoaderServiceClient loaderServiceClient;

    @Autowired // Inject Jackson ObjectMapper for JSON serialization (used in checksum calculation)
    private ObjectMapper objectMapper;

    /**
     * Listen to all supply chain Kafka topics and process incoming events.
     * This is the main entry point for all event consumption in the system.
     *
     * The @KafkaListener annotation subscribes to 4 topics simultaneously,
     * using a shared consumer group for load balancing across instances.
     * Manual acknowledgment mode ensures Level 1 deduplication via offset control.
     */
    @KafkaListener( // Declare this method as a Kafka message listener with the following configuration
        topics = { // Subscribe to all four supply chain event topics simultaneously
            "supply-chain.item.events",             // Item master data events (SKU-based)
            "supply-chain.trade-item.events",        // Trade item events (GTIN-based)
            "supply-chain.supplier-supply.events",   // Supplier supply level events
            "supply-chain.shipment-tracking.events"  // Shipment tracking events
        },
        groupId = "supply-chain-consumer-group", // Consumer group ID — all instances share partitions for load balancing
        containerFactory = "kafkaListenerContainerFactory" // Reference to the custom container factory configured in KafkaConsumerConfig
    )
    public void consume( // Method signature receives the deserialized message, topic name, and ack handle
            @Payload Map<String, Object> message, // Extract the message body as a generic Map (flexible schema)
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, // Extract the Kafka topic name from message headers
            Acknowledgment acknowledgment) { // Manual acknowledgment handle for committing offsets (Level 1 dedup)

        // Log the receipt of a new message with the source topic for traceability
        logger.info("📨 Received message from topic: {}", topic);

        try {
            // Determine the event type (item, trade-item, supplier-supply, shipment) from the topic name
            String eventType = extractEventType(topic);
            // Inject the resolved event type into the message map for downstream routing
            message.put("eventType", eventType);

            // Delegate to the event-type-specific processing method (includes Level 2-4 dedup)
            processEvent(message, eventType);

            // Level 1 Dedup: Manually commit the Kafka offset only after successful processing
            // If processing fails, the offset is NOT committed, so the message will be redelivered
            acknowledgment.acknowledge();
            // Log successful processing and offset commit
            logger.info("✅ Event processed and acknowledged");

        } catch (Exception e) {
            // Log the processing failure with full exception details for debugging
            logger.error("❌ Error processing event: {}", e.getMessage(), e);
            // Persist the error details to MongoDB error_logs collection for monitoring UI
            logError(message, "KAFKA_CONSUME", e);
            // Do NOT acknowledge — Kafka will redeliver this message on the next poll cycle
        }
    }

    /**
     * Route the event to the appropriate type-specific processing method.
     * Uses a switch statement on the event type string to dispatch processing.
     *
     * @param message   The event payload as a generic Map
     * @param eventType The resolved event type (item, trade-item, supplier-supply, shipment)
     * @throws Exception Propagated from type-specific processors for error handling in consume()
     */
    private void processEvent(Map<String, Object> message, String eventType) throws Exception {
        // Switch on the event type to route to the correct handler method
        switch (eventType) {
            case "item": // Route item events to the item processing pipeline
                processItemEvent(message);
                break;
            case "trade-item": // Route trade item events to the trade item processing pipeline
                processTradeItemEvent(message);
                break;
            case "supplier-supply": // Route supplier supply events to the supplier supply processing pipeline
                processSupplierSupplyEvent(message);
                break;
            case "shipment": // Route shipment events to the shipment processing pipeline
                processShipmentEvent(message);
                break;
            default: // Log a warning for any unrecognized event types (no processing occurs)
                logger.warn("⚠️ Unknown event type: {}", eventType);
        }
    }

    /**
     * Process an item event through the full 4-level deduplication pipeline.
     *
     * Flow: Level 2 (event ID) → Level 3 (SKU business key) → Level 4 (content hash)
     *       → Save to MongoDB → Forward to loader service
     *
     * @param message The item event payload containing skuId, itemName, price, etc.
     * @throws Exception If database save or forwarding fails
     */
    private void processItemEvent(Map<String, Object> message) throws Exception {
        // Extract the unique event identifier for Level 2 dedup check
        String eventId = (String) message.get("eventId");
        // Extract the SKU ID which serves as the business key for Level 3 dedup
        String skuId = (String) message.get("skuId");

        // Log the start of item event processing with key identifiers
        logger.info("Processing ITEM event - eventId: {}, skuId: {}", eventId, skuId);

        // Level 2 Dedup: Query MongoDB to check if this exact event ID has been processed before
        if (itemRepository.findByEventId(eventId).isPresent()) {
            // Event already processed — skip to avoid duplicate persistence
            logger.info("⏭️  DUPLICATE (Level 2): Event already processed - eventId: {}", eventId);
            return; // Exit early — the event will still be acknowledged in the calling method
        }

        // Level 3 Dedup: Look up any existing item with the same SKU business key
        var existingItem = itemRepository.findBySkuId(skuId);

        // Level 4 Dedup: Compute SHA-256 hash of the entire message payload
        String newChecksum = calculateChecksum(message);
        // If an item with this SKU exists AND its content hash matches, the data hasn't changed
        if (existingItem.isPresent() &&
            newChecksum.equals(existingItem.get().getDataChecksum())) {
            // Content is identical — no need to update the database
            logger.info("⏭️  NO CHANGES (Level 4): Data identical - skuId: {}", skuId);
            return; // Exit early — skip unnecessary database write
        }

        // All dedup checks passed — build the ItemEntity for MongoDB persistence
        ItemEntity entity = ItemEntity.builder()
            .eventId(eventId)                                     // Store event ID for Level 2 dedup lookups
            .skuId(skuId)                                         // Store SKU as business key for Level 3 dedup
            .itemName((String) message.get("itemName"))           // Extract and store the item display name
            .category((String) message.get("category"))           // Extract and store the product category
            .price(getDoubleValue(message, "price"))              // Safely extract price with type conversion
            .weight(getDoubleValue(message, "weight"))            // Safely extract weight with type conversion
            .dimensions((String) message.get("dimensions"))       // Extract physical dimensions string
            .status((String) message.get("status"))               // Extract item lifecycle status (ACTIVE, INACTIVE, etc.)
            .createdAt(LocalDateTime.now())                       // Set creation timestamp to current server time
            .updatedAt(LocalDateTime.now())                       // Set update timestamp (same as created for new records)
            .sourceSystem((String) message.get("sourceSystem"))   // Record which upstream system generated this event
            .processingStatus(ProcessingStatus.SUCCESS)           // Mark as successfully processed
            .dataChecksum(newChecksum)                            // Store SHA-256 hash for future Level 4 dedup checks
            .build(); // Build the immutable entity object using Lombok's builder pattern

        // Persist the entity to MongoDB 'items' collection (upsert by business key)
        itemRepository.save(entity);
        // Log successful database persistence with the business key
        logger.info("💾 Saved to MongoDB - skuId: {}", skuId);

        // Forward the event to the loader service for downstream routing to item-service
        try {
            loaderServiceClient.forwardToLoader(message); // HTTP POST to loader-service /api/loader/route
            logger.info("➡️  Forwarded to loader service"); // Log successful forwarding
        } catch (Exception e) {
            // Loader failure is non-fatal — the event is already persisted in MongoDB
            logger.error("Failed to forward to loader: {}", e.getMessage());
            // Log the loader failure to error_logs for monitoring and potential retry
            logError(message, "LOADER_SERVICE", e);
        }
    }

    /**
     * Process a trade item event through the 4-level deduplication pipeline.
     * Uses GTIN (Global Trade Item Number) as the Level 3 business key.
     *
     * @param message The trade item event payload containing gtin, supplierId, etc.
     * @throws Exception If database save or forwarding fails
     */
    private void processTradeItemEvent(Map<String, Object> message) throws Exception {
        // Extract the unique event identifier for Level 2 dedup
        String eventId = (String) message.get("eventId");
        // Extract GTIN which serves as the business key for trade items (Level 3)
        String gtin = (String) message.get("gtin");

        // Log the start of trade item processing with key identifiers
        logger.info("Processing TRADE_ITEM event - eventId: {}, gtin: {}", eventId, gtin);

        // Level 2 Dedup: Check if this event ID has already been processed
        if (tradeItemRepository.findByEventId(eventId).isPresent()) {
            logger.info("⏭️  DUPLICATE (Level 2): eventId: {}", eventId);
            return; // Skip duplicate event
        }

        // Level 3 & 4 Dedup: Find existing trade item by GTIN and compare content hash
        var existing = tradeItemRepository.findByGtin(gtin);
        // Compute SHA-256 hash of the current message for content comparison
        String newChecksum = calculateChecksum(message);
        // If GTIN exists and content hash matches, data is unchanged — skip processing
        if (existing.isPresent() && newChecksum.equals(existing.get().getDataChecksum())) {
            logger.info("⏭️  NO CHANGES (Level 4): gtin: {}", gtin);
            return; // Exit early — no data changes detected
        }

        // Build the TradeItemEntity for MongoDB persistence with all event fields mapped
        TradeItemEntity entity = TradeItemEntity.builder()
            .eventId(eventId)                                         // Store event ID for Level 2 dedup
            .gtin(gtin)                                               // Store GTIN as the document ID / business key
            .skuId((String) message.get("skuId"))                     // Link to the parent item via SKU
            .supplierId((String) message.get("supplierId"))           // Store the supplier identifier
            .supplierName((String) message.get("supplierName"))       // Store the supplier display name
            .description((String) message.get("description"))         // Store the product description
            .unitOfMeasure((String) message.get("unitOfMeasure"))     // Store UOM (EACH, CASE, PALLET, etc.)
            .minOrderQuantity(getIntValue(message, "minOrderQuantity")) // Safely extract min order qty with type conversion
            .leadTimeDays(getIntValue(message, "leadTimeDays"))       // Safely extract lead time in days
            .createdAt(LocalDateTime.now())                           // Set creation timestamp
            .updatedAt(LocalDateTime.now())                           // Set update timestamp
            .sourceSystem((String) message.get("sourceSystem"))       // Record the originating system
            .processingStatus(ProcessingStatus.SUCCESS)               // Mark as successfully processed
            .dataChecksum(newChecksum)                                // Store content hash for Level 4 dedup
            .build(); // Build the entity using Lombok builder pattern

        // Persist the trade item entity to MongoDB 'trade_items' collection
        tradeItemRepository.save(entity);
        logger.info("💾 Saved trade item - gtin: {}", gtin);

        // Forward to loader service for downstream routing to trade-item-service (port 8084)
        loaderServiceClient.forwardToLoader(message);
    }

    /**
     * Process a supplier supply event through the 4-level deduplication pipeline.
     * Uses supplier ID as the Level 3 business key.
     *
     * @param message The supplier supply event payload containing supplierId, warehouseId, quantities, etc.
     * @throws Exception If database save or forwarding fails
     */
    private void processSupplierSupplyEvent(Map<String, Object> message) throws Exception {
        // Extract the unique event identifier for Level 2 dedup
        String eventId = (String) message.get("eventId");
        // Extract supplier ID which serves as the business key (Level 3)
        String supplierId = (String) message.get("supplierId");
        // Extract warehouse ID for logging context
        String warehouseId = (String) message.get("warehouseId");

        // Log the start of supplier supply processing with all key identifiers
        logger.info("Processing SUPPLIER SUPPLY event - eventId: {},supplierId: {}, warehouse: {}", eventId, supplierId, warehouseId);

        // Level 2 Dedup: Check if this event ID has already been processed
        if (supplierSupplyRepository.findByEventId(eventId).isPresent()) {
            logger.info("⏭️  DUPLICATE (Level 2): eventId: {}", eventId);
            return; // Skip duplicate event — already processed
        }

        // Level 3 & 4 Dedup: Find existing record by supplier ID and compare content hash
        var existing = supplierSupplyRepository.findBySupplierId(supplierId);
        // Compute SHA-256 content hash of the current message
        String newChecksum = calculateChecksum(message);
        // If supplier ID exists and hash matches, data hasn't changed — skip update
        if (existing.isPresent() && newChecksum.equals(existing.get().getDataChecksum())) {
            logger.info("⏭️  NO CHANGES (Level 4)");
            return; // Exit early — identical content detected
        }

        // Build the SupplierSupplyEntity for MongoDB persistence
        SupplierSupplyEntity entity = SupplierSupplyEntity.builder()
            .eventId(eventId)                                             // Store event ID for Level 2 dedup
            .supplierId(supplierId)                                       // Store supplier ID as document ID / business key
            .warehouseId(warehouseId)                                     // Store warehouse identifier
                .skuId((String) message.get("skuId"))                     // Link to item via SKU ID
            .warehouseName((String) message.get("warehouseName"))         // Store warehouse display name
            .availableQuantity(getIntValue(message, "availableQuantity")) // Extract available stock count
            .reservedQuantity(getIntValue(message, "reservedQuantity"))   // Extract reserved/allocated stock count
            .onOrderQuantity(getIntValue(message, "onOrderQuantity"))     // Extract quantity currently on order
            .reorderPoint(getIntValue(message, "reorderPoint"))           // Extract minimum stock threshold for reorder alerts
            .reorderQuantity(getIntValue(message, "reorderQuantity"))     // Extract standard reorder quantity
            .createdAt(LocalDateTime.now())                               // Set creation timestamp
            .updatedAt(LocalDateTime.now())                               // Set update timestamp
            .sourceSystem((String) message.get("sourceSystem"))           // Record originating system
            .processingStatus(ProcessingStatus.SUCCESS)                   // Mark as successfully processed
            .dataChecksum(newChecksum)                                    // Store content hash for Level 4 dedup
            .build(); // Build entity using Lombok builder

        // Persist the supplier supply entity to MongoDB 'supplier_supply' collection
        supplierSupplyRepository.save(entity);
        logger.info("💾 Saved supplier supply"); // Log successful persistence

        // Forward to loader service for downstream routing to supplier-supply-service (port 8085)
        loaderServiceClient.forwardToLoader(message);
    }

    /**
     * Process a shipment event through the 4-level deduplication pipeline.
     * Uses tracking number as the Level 3 business key.
     *
     * @param message The shipment event payload containing trackingNumber, carrier, status, locations, etc.
     * @throws Exception If database save or forwarding fails
     */
    private void processShipmentEvent(Map<String, Object> message) throws Exception {
        // Extract the unique event identifier for Level 2 dedup
        String eventId = (String) message.get("eventId");
        // Extract tracking number which serves as the business key for shipments (Level 3)
        String trackingNumber = (String) message.get("trackingNumber");

        // Log the start of shipment processing with the tracking number
        logger.info("Processing SHIPMENT event - tracking: {}", trackingNumber);

        // Level 2 Dedup: Check if this event ID has already been processed
        if (shipmentRepository.findByEventId(eventId).isPresent()) {
            logger.info("⏭️  DUPLICATE (Level 2): eventId: {}", eventId);
            return; // Skip duplicate event
        }

        // Level 3 & 4 Dedup: Find existing shipment by tracking number and compare content hash
        var existing = shipmentRepository.findByTrackingNumber(trackingNumber);
        // Compute SHA-256 content hash for comparison
        String newChecksum = calculateChecksum(message);
        // If tracking number exists and hash matches, data is unchanged — skip
        if (existing.isPresent() && newChecksum.equals(existing.get().getDataChecksum())) {
            logger.info("⏭️  NO CHANGES (Level 4)");
            return; // Exit early — identical content
        }

        // Build the ShipmentEntity for MongoDB persistence with all shipment fields
        ShipmentEntity entity = ShipmentEntity.builder()
            .eventId(eventId)                                                   // Store event ID for Level 2 dedup
            .trackingNumber(trackingNumber)                                     // Store tracking number as business key
            .orderId((String) message.get("orderId"))                           // Link to the originating order
            .carrier((String) message.get("carrier"))                           // Store shipping carrier name (FedEx, UPS, etc.)
            .shipmentStatus((String) message.get("shipmentStatus"))             // Store current shipment status
            .originLocation((String) message.get("originLocation"))             // Store origin/ship-from location
            .destinationLocation((String) message.get("destinationLocation"))   // Store destination/ship-to location
            .currentLocation((String) message.get("currentLocation"))           // Store current in-transit location
            .createdAt(LocalDateTime.now())                                     // Set creation timestamp
            .updatedAt(LocalDateTime.now())                                     // Set update timestamp
            .sourceSystem((String) message.get("sourceSystem"))                 // Record originating system
            .processingStatus(ProcessingStatus.SUCCESS)                         // Mark as successfully processed
            .dataChecksum(newChecksum)                                          // Store content hash for Level 4 dedup
            .build(); // Build entity using Lombok builder

        // Persist the shipment entity to MongoDB 'shipments' collection
        shipmentRepository.save(entity);
        logger.info("💾 Saved shipment"); // Log successful persistence

        // Forward to loader service for downstream routing to shipment-service (port 8086)
        loaderServiceClient.forwardToLoader(message);
    }

    /**
     * Calculate a SHA-256 checksum of the event data for Level 4 deduplication.
     * Serializes the Map to JSON, then computes the SHA-256 digest.
     * This allows detecting whether the actual content has changed between events
     * that share the same business key.
     *
     * @param data The event payload map to hash
     * @return Hexadecimal string representation of the SHA-256 hash, or empty string on error
     */
    private String calculateChecksum(Map<String, Object> data) {
        try {
            // Serialize the entire event map to a JSON string for consistent hashing
            String json = objectMapper.writeValueAsString(data);
            // Create a SHA-256 MessageDigest instance for cryptographic hashing
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Compute the hash of the JSON bytes using UTF-8 encoding
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            // Convert the raw byte array to a hexadecimal string representation
            return bytesToHex(hash);
        } catch (Exception e) {
            // Log the error but don't throw — return empty string as a fallback
            logger.error("Failed to calculate checksum", e);
            return ""; // Empty string ensures dedup check won't match, forcing a save
        }
    }

    /**
     * Convert a byte array to its hexadecimal string representation.
     * Each byte is formatted as a two-character hex value (e.g., 0x0A → "0a").
     *
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation of the input bytes
     */
    private String bytesToHex(byte[] bytes) {
        // StringBuilder for efficient string concatenation in a loop
        StringBuilder sb = new StringBuilder();
        // Iterate over each byte in the hash output
        for (byte b : bytes) {
            // Format each byte as a 2-digit hex value and append to the builder
            sb.append(String.format("%02x", b));
        }
        // Return the complete hexadecimal string
        return sb.toString();
    }

    /**
     * Extract the event type from the Kafka topic name.
     * Maps topic names to event type strings used for routing and processing.
     *
     * Topic name patterns:
     * - supply-chain.item.events → "item"
     * - supply-chain.trade-item.events → "trade-item"
     * - supply-chain.supplier-supply.events → "supplier-supply"
     * - supply-chain.shipment-tracking.events → "shipment"
     *
     * @param topic The Kafka topic name
     * @return The resolved event type string, or "unknown" if no match
     */
    private String extractEventType(String topic) {
        // Check for trade-item first (before item) because "item" is a substring of "trade-item"
        if (topic.contains("trade-item")) return "trade-item";
        // Check for supplier-supply events
        if (topic.contains("supplier-supply")) return "supplier-supply";
        // Check for shipment tracking events
        if (topic.contains("shipment")) return "shipment";
        // Check for item events last (most general match — "item.events" avoids matching "trade-item")
        if (topic.contains("item.events")) return "item";
        // Return "unknown" for any unrecognized topic (hits default case in processEvent)
        return "unknown";
    }

    /**
     * Log a processing error to MongoDB's error_logs collection.
     * Creates an ErrorLog document with full context for the monitoring UI
     * and potential automated retry mechanisms.
     *
     * @param eventData The original event data that caused the error
     * @param stage     The processing stage where failure occurred (KAFKA_CONSUME, DB_SAVE, LOADER_SERVICE)
     * @param e         The exception that was thrown
     */
    private void logError(Map<String, Object> eventData, String stage, Exception e) {
        try {
            // Build an ErrorLog entity with all relevant context for debugging and retry
            ErrorLog errorLog = ErrorLog.builder()
                .eventId((String) eventData.get("eventId"))   // Link error to the specific event ID
                .eventType((String) eventData.get("eventType")) // Record the event type for filtering
                .failureStage(stage)                           // Record which stage failed (DB_SAVE, OSP_API, LOADER_SERVICE)
                .errorMessage(e.getMessage())                  // Store the exception message for quick diagnosis
                .stackTrace(getStackTrace(e))                  // Store truncated stack trace for detailed debugging
                .eventData(eventData)                          // Store the original event payload for retry
                .failedAt(LocalDateTime.now())                 // Timestamp when the failure occurred
                .resolved(false)                               // Mark as unresolved — needs attention from operations
                .retryCount(0)                                 // Initialize retry counter at zero
                .build(); // Build the ErrorLog entity

            // Persist the error log to MongoDB 'error_logs' collection
            errorLogRepository.save(errorLog);
        } catch (Exception ex) {
            // If error logging itself fails, fall back to console logging to avoid infinite loops
            logger.error("Failed to log error: {}", ex.getMessage());
        }
    }

    /**
     * Extract and truncate a stack trace from an exception.
     * Limits output to 1000 characters to prevent excessive document size in MongoDB.
     *
     * @param e The exception to extract the stack trace from
     * @return Truncated string representation of the stack trace
     */
    private String getStackTrace(Exception e) {
        // Use StringWriter to capture the full stack trace as a string
        java.io.StringWriter sw = new java.io.StringWriter();
        // PrintWriter wraps StringWriter for the printStackTrace() method
        e.printStackTrace(new java.io.PrintWriter(sw));
        // Truncate to 1000 characters maximum to prevent MongoDB document bloat
        return sw.toString().substring(0, Math.min(sw.toString().length(), 1000));
    }

    /**
     * Safely extract a Double value from the event map with type conversion.
     * Handles both Number types (from JSON deserialization) and String representations.
     *
     * @param map The event payload map
     * @param key The field name to extract
     * @return The Double value, or null if the key is absent or value is null
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        // Retrieve the raw value from the map (may be null)
        Object value = map.get(key);
        // Return null for missing or null values (nullable field)
        if (value == null) return null;
        // If already a Number type (Integer, Long, Double), convert via doubleValue()
        if (value instanceof Number) return ((Number) value).doubleValue();
        // Fallback: parse from String representation
        return Double.parseDouble(value.toString());
    }

    /**
     * Safely extract an Integer value from the event map with type conversion.
     * Handles both Number types (from JSON deserialization) and String representations.
     *
     * @param map The event payload map
     * @param key The field name to extract
     * @return The Integer value, or null if the key is absent or value is null
     */
    private Integer getIntValue(Map<String, Object> map, String key) {
        // Retrieve the raw value from the map (may be null)
        Object value = map.get(key);
        // Return null for missing or null values (nullable field)
        if (value == null) return null;
        // If already a Number type (Integer, Long, Double), convert via intValue()
        if (value instanceof Number) return ((Number) value).intValue();
        // Fallback: parse from String representation
        return Integer.parseInt(value.toString());
    }
}
