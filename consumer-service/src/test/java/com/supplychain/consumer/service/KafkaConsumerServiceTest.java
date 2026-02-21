// Package declaration: places this test class in the consumer service's service package
package com.supplychain.consumer.service;

// Import Jackson's ObjectMapper for JSON serialization/deserialization in test checksum calculation
import com.fasterxml.jackson.databind.ObjectMapper;
// Import all entity classes used by the consumer: ItemEntity, TradeItemEntity, SupplierSupplyEntity, ShipmentEntity, ErrorLog, ProcessingStatus
import com.supplychain.consumer.entity.*;
// Import all repository interfaces used by the consumer: ItemRepository, TradeItemRepository, SupplierSupplyRepository, ShipmentRepository, ErrorLogRepository
import com.supplychain.consumer.repository.*;
// Import JUnit 5 @BeforeEach annotation for setup methods that run before each test
import org.junit.jupiter.api.BeforeEach;
// Import JUnit 5 @Test annotation to mark methods as test cases
import org.junit.jupiter.api.Test;
// Import JUnit 5 extension mechanism for integrating Mockito with JUnit 5
import org.junit.jupiter.api.extension.ExtendWith;
// Import Mockito's ArgumentCaptor to capture and inspect arguments passed to mocked methods
import org.mockito.ArgumentCaptor;
// Import @InjectMocks annotation to create an instance with all @Mock fields injected
import org.mockito.InjectMocks;
// Import @Mock annotation to create mock instances of dependencies
import org.mockito.Mock;
// Import @Spy annotation to create a partial mock that delegates to the real implementation by default
import org.mockito.Spy;
// Import MockitoExtension to enable Mockito annotations (@Mock, @InjectMocks, etc.) in JUnit 5
import org.mockito.junit.jupiter.MockitoExtension;
// Import Spring Kafka's Acknowledgment interface used for manual offset commit control
import org.springframework.kafka.support.Acknowledgment;

// Import HashMap for creating mutable test event message maps
import java.util.HashMap;
// Import Map interface for event message payloads (key-value pairs representing JSON fields)
import java.util.Map;
// Import Optional for wrapping repository lookup results (present = found, empty = not found)
import java.util.Optional;

// Import AssertJ's assertThat for fluent, readable assertions on captured entity fields
import static org.assertj.core.api.Assertions.assertThat;
// Import Mockito's any() matcher for matching any argument of a given type in mock verification
import static org.mockito.ArgumentMatchers.any;
// Import Mockito's anyString() matcher for matching any String argument in mock stubbing
import static org.mockito.ArgumentMatchers.anyString;
// Import all static Mockito methods: mock(), when(), verify(), never(), doThrow(), etc.
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaConsumerService}.
 *
 * Tests the core Kafka consumer logic with 4-level deduplication:
 * - Level 1: Kafka offset management (manual acknowledgment after success)
 * - Level 2: Event ID uniqueness check (database lookup)
 * - Level 3: Business key check (SKU, GTIN, supplier ID, tracking number)
 * - Level 4: SHA-256 content hash comparison (detects actual data changes)
 *
 * Also tests:
 * - Event type extraction from Kafka topic names
 * - Entity field mapping from message to MongoDB document
 * - Error logging to MongoDB error_logs collection
 * - Forwarding events to the loader service after save
 *
 * All dependencies (5 repositories, LoaderServiceClient, ObjectMapper) are mocked.
 */
// @ExtendWith(MockitoExtension.class) integrates Mockito with JUnit 5, enabling @Mock and @InjectMocks annotations
@ExtendWith(MockitoExtension.class)
// Declare the test class with package-private visibility (no public modifier needed for JUnit 5)
class KafkaConsumerServiceTest {

    // Mock the ItemRepository dependency; intercepts all calls to item database operations
    @Mock private ItemRepository itemRepository;
    // Mock the TradeItemRepository dependency; intercepts all calls to trade item database operations
    @Mock private TradeItemRepository tradeItemRepository;
    // Mock the SupplierSupplyRepository dependency; intercepts all calls to supplier supply database operations
    @Mock private SupplierSupplyRepository supplierSupplyRepository;
    // Mock the ShipmentRepository dependency; intercepts all calls to shipment database operations
    @Mock private ShipmentRepository shipmentRepository;
    // Mock the ErrorLogRepository dependency; intercepts all calls to error log database operations
    @Mock private ErrorLogRepository errorLogRepository;
    // Mock the LoaderServiceClient dependency; intercepts all calls that forward events to the loader service
    @Mock private LoaderServiceClient loaderServiceClient;
    // Mock the Kafka Acknowledgment interface; tracks whether acknowledge() was called for offset commit
    @Mock private Acknowledgment acknowledgment;
    // @Spy creates a partial mock of ObjectMapper: real JSON serialization is used, but calls can be verified
    // Initialized with a real ObjectMapper instance so calculateChecksum() works correctly in tests
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    // @InjectMocks creates an instance of KafkaConsumerService and injects all @Mock and @Spy fields into it
    @InjectMocks
    // The service under test: KafkaConsumerService handles Kafka message consumption and deduplication
    private KafkaConsumerService service;

    // ==================== Event Type Extraction from Topic ====================

    /**
     * Verifies that messages from the item topic are routed to item processing
     * and saved to the item repository.
     */
    // @Test marks this method as a JUnit 5 test case that will be executed by the test runner
    @Test
    // Test method: verifies item topic messages are consumed, saved, and acknowledged
    void consume_itemTopic_processesAsItemEvent() {
        // Create a test item event message with all required fields
        Map<String, Object> message = itemMessage();
        // Stub: Level 2 dedup check returns empty (no duplicate event ID found in database)
        when(itemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup check returns empty (no duplicate SKU business key found)
        when(itemRepository.findBySkuId(anyString())).thenReturn(Optional.empty());
        // Stub: save() returns null (return value not used by the service)
        when(itemRepository.save(any())).thenReturn(null);

        // Act: invoke the consumer with the item message, item topic name, and mock acknowledgment
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Assert: verify that an ItemEntity was saved to the item repository exactly once
        verify(itemRepository).save(any(ItemEntity.class));
        // Assert: verify that the Kafka offset was acknowledged (committed) after successful processing
        verify(acknowledgment).acknowledge();
    }

    /**
     * Verifies that messages from the trade-item topic are routed to trade item processing
     * and saved to the trade item repository.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: verifies trade-item topic messages are consumed and saved correctly
    void consume_tradeItemTopic_processesAsTradeItemEvent() {
        // Create a test trade item event message with GTIN and supplier fields
        Map<String, Object> message = tradeItemMessage();
        // Stub: Level 2 dedup - no duplicate event ID exists for this trade item
        when(tradeItemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup - no duplicate GTIN business key exists
        when(tradeItemRepository.findByGtin(anyString())).thenReturn(Optional.empty());
        // Stub: save() returns null (return value not needed)
        when(tradeItemRepository.save(any())).thenReturn(null);

        // Act: invoke the consumer with trade-item topic name to trigger trade item routing
        service.consume(message, "supply-chain.trade-item.events", acknowledgment);

        // Assert: verify that a TradeItemEntity was persisted to the trade item repository
        verify(tradeItemRepository).save(any(TradeItemEntity.class));
        // Assert: verify the Kafka offset was committed after successful processing
        verify(acknowledgment).acknowledge();
    }

    /**
     * Verifies that messages from the supplier-supply topic are routed to supplier supply
     * processing and saved to the supplier supply repository using supplier ID as business key.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: verifies supplier-supply topic messages are consumed and saved correctly
    void consume_supplierSupplyTopic_processesAsSupplierSupplyEvent() {
        // Create a test supplier supply event message with warehouse and quantity data
        Map<String, Object> message = supplierSupplyMessage();
        // Stub: Level 2 dedup - no duplicate event ID exists for this supplier supply event
        when(supplierSupplyRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup - no duplicate supplier ID business key exists
        when(supplierSupplyRepository.findBySupplierId(anyString())).thenReturn(Optional.empty());
        // Stub: save() returns null (return value not needed)
        when(supplierSupplyRepository.save(any())).thenReturn(null);

        // Act: invoke the consumer with supplier-supply topic name
        service.consume(message, "supply-chain.supplier-supply.events", acknowledgment);

        // Assert: verify that a SupplierSupplyEntity was persisted to the supplier supply repository
        verify(supplierSupplyRepository).save(any(SupplierSupplyEntity.class));
        // Assert: verify the Kafka offset was committed after successful processing
        verify(acknowledgment).acknowledge();
    }

    /**
     * Verifies that messages from the shipment-tracking topic are routed to shipment processing
     * and saved to the shipment repository.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: verifies shipment-tracking topic messages are consumed and saved correctly
    void consume_shipmentTopic_processesAsShipmentEvent() {
        // Create a test shipment event message with tracking and carrier information
        Map<String, Object> message = shipmentMessage();
        // Stub: Level 2 dedup - no duplicate event ID exists for this shipment event
        when(shipmentRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup - no duplicate tracking number business key exists
        when(shipmentRepository.findByTrackingNumber(anyString())).thenReturn(Optional.empty());
        // Stub: save() returns null (return value not needed)
        when(shipmentRepository.save(any())).thenReturn(null);

        // Act: invoke the consumer with shipment-tracking topic name
        service.consume(message, "supply-chain.shipment-tracking.events", acknowledgment);

        // Assert: verify that a ShipmentEntity was persisted to the shipment repository
        verify(shipmentRepository).save(any(ShipmentEntity.class));
        // Assert: verify the Kafka offset was committed after successful processing
        verify(acknowledgment).acknowledge();
    }

    // ==================== Level 2 Dedup: Event ID ====================

    /** Verifies that a duplicate item event ID causes the message to be skipped (not saved again). */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when an item with the same event ID already exists, save should be skipped
    void consume_duplicateItemEventId_skipsProcessing() {
        // Create a test item message with eventId "evt_item_001"
        Map<String, Object> message = itemMessage();
        // Stub: Level 2 dedup returns an existing entity, indicating this event ID was already processed
        when(itemRepository.findByEventId("evt_item_001")).thenReturn(Optional.of(new ItemEntity()));

        // Act: attempt to consume a message with a duplicate event ID
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Assert: verify that save() was NEVER called (duplicate was detected and skipped)
        verify(itemRepository, never()).save(any());
        // Assert: verify the offset is still acknowledged (duplicate is not an error, just skip it)
        verify(acknowledgment).acknowledge();
    }

    /** Verifies that a duplicate trade item event ID causes the message to be skipped. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when a trade item with the same event ID already exists, save should be skipped
    void consume_duplicateTradeItemEventId_skipsProcessing() {
        // Create a test trade item message with eventId "evt_trade_001"
        Map<String, Object> message = tradeItemMessage();
        // Stub: Level 2 dedup returns an existing entity for the trade item event ID
        when(tradeItemRepository.findByEventId("evt_trade_001")).thenReturn(Optional.of(new TradeItemEntity()));

        // Act: attempt to consume a message with a duplicate trade item event ID
        service.consume(message, "supply-chain.trade-item.events", acknowledgment);

        // Assert: verify that save() was NEVER called (duplicate detected)
        verify(tradeItemRepository, never()).save(any());
        // Assert: verify the offset is still acknowledged
        verify(acknowledgment).acknowledge();
    }

    /** Verifies that a duplicate supplier supply event ID causes the message to be skipped. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when a supplier supply with the same event ID already exists, save should be skipped
    void consume_duplicateSupplierSupplyEventId_skipsProcessing() {
        // Create a test supplier supply message with eventId "evt_sup_001"
        Map<String, Object> message = supplierSupplyMessage();
        // Stub: Level 2 dedup returns an existing entity for the supplier supply event ID
        when(supplierSupplyRepository.findByEventId("evt_sup_001")).thenReturn(Optional.of(new SupplierSupplyEntity()));

        // Act: attempt to consume a message with a duplicate supplier supply event ID
        service.consume(message, "supply-chain.supplier-supply.events", acknowledgment);

        // Assert: verify that save() was NEVER called (duplicate detected)
        verify(supplierSupplyRepository, never()).save(any());
        // Assert: verify the offset is still acknowledged
        verify(acknowledgment).acknowledge();
    }

    /** Verifies that a duplicate shipment event ID causes the message to be skipped. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when a shipment with the same event ID already exists, save should be skipped
    void consume_duplicateShipmentEventId_skipsProcessing() {
        // Create a test shipment message with eventId "evt_ship_001"
        Map<String, Object> message = shipmentMessage();
        // Stub: Level 2 dedup returns an existing entity for the shipment event ID
        when(shipmentRepository.findByEventId("evt_ship_001")).thenReturn(Optional.of(new ShipmentEntity()));

        // Act: attempt to consume a message with a duplicate shipment event ID
        service.consume(message, "supply-chain.shipment-tracking.events", acknowledgment);

        // Assert: verify that save() was NEVER called (duplicate detected)
        verify(shipmentRepository, never()).save(any());
        // Assert: verify the offset is still acknowledged
        verify(acknowledgment).acknowledge();
    }

    // ==================== Level 4 Dedup: Content Hash ====================

    /**
     * Verifies that when an item with the same business key exists and the content hash
     * matches (no data changes), the save is skipped.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when the SHA-256 content hash matches the existing entity, skip the save
    void consume_sameContentHash_skipsItem() throws Exception {
        // Create a test item message
        Map<String, Object> message = itemMessage();
        // The consume method adds eventType before checksum is calculated, so we replicate that
        message.put("eventType", "item");
        // Calculate the SHA-256 hash of the message to match what the service will compute
        String hash = calculateTestChecksum(message);

        // Create an existing entity with the same checksum to simulate identical content
        ItemEntity existing = new ItemEntity();
        // Set the data checksum on the existing entity to match the incoming message
        existing.setDataChecksum(hash);

        // Stub: Level 2 dedup returns empty (event ID is new, passes this check)
        when(itemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup finds an existing entity by SKU (business key match)
        when(itemRepository.findBySkuId("SKU001")).thenReturn(Optional.of(existing));

        // Act: consume the message; Level 4 dedup should detect matching hash and skip save
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Assert: verify that save() was NEVER called because the content hash matched
        verify(itemRepository, never()).save(any());
        // Assert: verify the offset is acknowledged (no error, just a content-identical duplicate)
        verify(acknowledgment).acknowledge();
    }

    /**
     * Verifies that when an item with the same business key exists but the content hash
     * differs (data changed), the item is saved with updated data.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when the content hash differs from the existing entity, the update should be saved
    void consume_differentContentHash_savesItem() throws Exception {
        // Create a test item message
        Map<String, Object> message = itemMessage();

        // Create an existing entity with a different (old) checksum to simulate changed content
        ItemEntity existing = new ItemEntity();
        // Set an old hash that will NOT match the incoming message's computed hash
        existing.setDataChecksum("oldhash123");

        // Stub: Level 2 dedup returns empty (event ID is new)
        when(itemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup finds an existing entity by SKU (business key match)
        when(itemRepository.findBySkuId("SKU001")).thenReturn(Optional.of(existing));
        // Stub: save() returns null (return value not needed)
        when(itemRepository.save(any())).thenReturn(null);

        // Act: consume the message; Level 4 dedup detects different hash, so it should save
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Assert: verify that save() WAS called because the content changed (hash mismatch)
        verify(itemRepository).save(any(ItemEntity.class));
    }

    // ==================== Entity Field Mapping ====================

    /**
     * Verifies that all fields from the item message are correctly mapped to the ItemEntity.
     * Uses ArgumentCaptor to inspect the entity passed to repository.save().
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: verifies all message fields are correctly mapped to entity properties
    void consume_itemEvent_mapsAllFieldsToEntity() {
        // Create a test item message with all fields populated
        Map<String, Object> message = itemMessage();
        // Stub: Level 2 dedup returns empty (no duplicate event ID)
        when(itemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup returns empty (no duplicate SKU business key)
        when(itemRepository.findBySkuId(anyString())).thenReturn(Optional.empty());

        // Create an ArgumentCaptor to capture the ItemEntity object passed to save()
        ArgumentCaptor<ItemEntity> captor = ArgumentCaptor.forClass(ItemEntity.class);
        // Stub: save() captures the argument for later inspection and returns null
        when(itemRepository.save(captor.capture())).thenReturn(null);

        // Act: consume the item message from the item topic
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Retrieve the captured ItemEntity that was passed to save()
        ItemEntity saved = captor.getValue();
        // Assert: eventId field was correctly mapped from the message
        assertThat(saved.getEventId()).isEqualTo("evt_item_001");
        // Assert: skuId field was correctly mapped from the message
        assertThat(saved.getSkuId()).isEqualTo("SKU001");
        // Assert: itemName field was correctly mapped from the message
        assertThat(saved.getItemName()).isEqualTo("Laptop");
        // Assert: category field was correctly mapped from the message
        assertThat(saved.getCategory()).isEqualTo("Electronics");
        // Assert: price field was correctly mapped from the message (numeric value preserved)
        assertThat(saved.getPrice()).isEqualTo(999.99);
        // Assert: weight field was correctly mapped from the message (numeric value preserved)
        assertThat(saved.getWeight()).isEqualTo(2.5);
        // Assert: processingStatus was set to SUCCESS after successful processing
        assertThat(saved.getProcessingStatus()).isEqualTo(ProcessingStatus.SUCCESS);
        // Assert: dataChecksum was computed and is not empty (SHA-256 hash of the message content)
        assertThat(saved.getDataChecksum()).isNotEmpty();
    }

    /**
     * Verifies that shipment-specific fields (tracking number, carrier, status)
     * are correctly mapped to the ShipmentEntity.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: verifies shipment-specific fields are correctly mapped to entity properties
    void consume_shipmentEvent_mapsAllFieldsToEntity() {
        // Create a test shipment message with tracking number, carrier, and status
        Map<String, Object> message = shipmentMessage();
        // Stub: Level 2 dedup returns empty (no duplicate event ID)
        when(shipmentRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup returns empty (no duplicate tracking number)
        when(shipmentRepository.findByTrackingNumber(anyString())).thenReturn(Optional.empty());

        // Create an ArgumentCaptor to capture the ShipmentEntity passed to save()
        ArgumentCaptor<ShipmentEntity> captor = ArgumentCaptor.forClass(ShipmentEntity.class);
        // Stub: save() captures the argument for inspection
        when(shipmentRepository.save(captor.capture())).thenReturn(null);

        // Act: consume the shipment message from the shipment-tracking topic
        service.consume(message, "supply-chain.shipment-tracking.events", acknowledgment);

        // Retrieve the captured ShipmentEntity that was passed to save()
        ShipmentEntity saved = captor.getValue();
        // Assert: trackingNumber field was correctly mapped from the message
        assertThat(saved.getTrackingNumber()).isEqualTo("TRACK123");
        // Assert: carrier field was correctly mapped from the message
        assertThat(saved.getCarrier()).isEqualTo("FedEx");
        // Assert: shipmentStatus field was correctly mapped from the message
        assertThat(saved.getShipmentStatus()).isEqualTo("IN_TRANSIT");
    }

    // ==================== Loader Service Forwarding ====================

    /** Verifies that after saving to MongoDB, the event is forwarded to the loader service. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: after successful save, the raw message should be forwarded to the loader service
    void consume_afterSave_forwardsToLoader() {
        // Create a test item message
        Map<String, Object> message = itemMessage();
        // Stub: Level 2 dedup returns empty (no duplicate event ID)
        when(itemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup returns empty (no duplicate SKU)
        when(itemRepository.findBySkuId(anyString())).thenReturn(Optional.empty());
        // Stub: save() returns null
        when(itemRepository.save(any())).thenReturn(null);

        // Act: consume the item message
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Assert: verify that the loader service client was called with the original message
        verify(loaderServiceClient).forwardToLoader(message);
    }

    /**
     * Verifies that if the loader service fails, the event is still acknowledged
     * (already saved to MongoDB) and the failure is logged to error_logs.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when the loader service throws, the event is still acknowledged but the error is logged
    void consume_loaderFails_stillAcknowledges() {
        // Create a test item message
        Map<String, Object> message = itemMessage();
        // Stub: Level 2 dedup returns empty (no duplicate event ID)
        when(itemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Stub: Level 3 dedup returns empty (no duplicate SKU)
        when(itemRepository.findBySkuId(anyString())).thenReturn(Optional.empty());
        // Stub: save() returns null (event is saved to MongoDB successfully)
        when(itemRepository.save(any())).thenReturn(null);
        // Stub: loader service client throws a RuntimeException to simulate a downstream failure
        doThrow(new RuntimeException("Loader down")).when(loaderServiceClient).forwardToLoader(any());

        // Act: consume the item message; save succeeds but loader forwarding fails
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Assert: the Kafka offset is STILL acknowledged because the event was already saved to MongoDB
        verify(acknowledgment).acknowledge();
        // Assert: the loader failure is logged to the error_logs collection for monitoring
        verify(errorLogRepository).save(any(ErrorLog.class));
    }

    // ==================== Error Logging ====================

    /**
     * Verifies that when processing throws an exception, the offset is NOT acknowledged
     * (message will be redelivered) and the error is logged to MongoDB.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when the main processing fails (e.g., DB down), offset is NOT committed
    void consume_processingException_logsErrorAndDoesNotAcknowledge() {
        // Create a test item message
        Map<String, Object> message = itemMessage();
        // Stub: the event ID lookup throws a RuntimeException to simulate a database failure
        when(itemRepository.findByEventId(anyString())).thenThrow(new RuntimeException("DB down"));

        // Act: consume the item message; the DB lookup will throw
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Assert: the Kafka offset is NOT acknowledged; the message will be redelivered by Kafka
        verify(acknowledgment, never()).acknowledge();
        // Assert: the error is logged to MongoDB's error_logs collection for later investigation
        verify(errorLogRepository).save(any(ErrorLog.class));
    }

    /**
     * Verifies that the error log contains the correct failure stage (KAFKA_CONSUME),
     * is marked as unresolved, and has a retry count of 0.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: inspects the ErrorLog entity fields to ensure correct error metadata is captured
    void consume_processingException_errorLogContainsStage() {
        // Create a test item message
        Map<String, Object> message = itemMessage();
        // Stub: the event ID lookup throws a RuntimeException to simulate a database error
        when(itemRepository.findByEventId(anyString())).thenThrow(new RuntimeException("DB error"));

        // Create an ArgumentCaptor to capture the ErrorLog entity passed to save()
        ArgumentCaptor<ErrorLog> captor = ArgumentCaptor.forClass(ErrorLog.class);
        // Stub: error log save() captures the argument for inspection
        when(errorLogRepository.save(captor.capture())).thenReturn(null);

        // Act: consume the item message; processing will fail due to the DB error
        service.consume(message, "supply-chain.item.events", acknowledgment);

        // Retrieve the captured ErrorLog that was saved
        ErrorLog errorLog = captor.getValue();
        // Assert: the failure stage is "KAFKA_CONSUME" indicating where the error occurred in the pipeline
        assertThat(errorLog.getFailureStage()).isEqualTo("KAFKA_CONSUME");
        // Assert: the error is marked as unresolved (false) so it appears in the monitoring dashboard
        assertThat(errorLog.getResolved()).isFalse();
        // Assert: the retry count starts at 0 (this is the first failure, no retries yet)
        assertThat(errorLog.getRetryCount()).isEqualTo(0);
    }

    // ==================== Unknown Topic ====================

    /**
     * Verifies that messages from an unknown topic are acknowledged without processing.
     * The event type "unknown" hits the default case in processEvent().
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: messages from unrecognized topics should be acknowledged without saving
    void consume_unknownTopic_acknowledgesWithoutProcessing() {
        // Create a minimal message with just an event ID (no type-specific fields needed)
        Map<String, Object> message = new HashMap<>();
        // Set the event ID for the unknown message
        message.put("eventId", "evt_unknown_001");

        // Act: consume from an unrecognized topic name
        service.consume(message, "some.random.topic", acknowledgment);

        // Assert: no item was saved to any repository (unknown topic is not processed)
        verify(itemRepository, never()).save(any());
        // Assert: the offset is still acknowledged to prevent infinite redelivery of unknown messages
        verify(acknowledgment).acknowledge();
    }

    // ==================== Test Data Helpers ====================

    /** Creates a valid item event message with all fields for testing. */
    // Helper method to create a complete item event payload used across multiple test cases
    private Map<String, Object> itemMessage() {
        // Create a mutable HashMap to allow field modification in individual tests
        Map<String, Object> msg = new HashMap<>();
        // Set the event ID used for Level 2 deduplication
        msg.put("eventId", "evt_item_001");
        // Set the SKU ID used as the business key for Level 3 deduplication
        msg.put("skuId", "SKU001");
        // Set the item name field for the entity
        msg.put("itemName", "Laptop");
        // Set the category field for the entity
        msg.put("category", "Electronics");
        // Set the price field (numeric) for the entity
        msg.put("price", 999.99);
        // Set the weight field (numeric) for the entity
        msg.put("weight", 2.5);
        // Set the dimensions field for the entity
        msg.put("dimensions", "10x10x10");
        // Set the status field for the entity
        msg.put("status", "ACTIVE");
        // Set the source system identifier for provenance tracking
        msg.put("sourceSystem", "test");
        // Return the fully populated item message map
        return msg;
    }

    /** Creates a valid trade item event message with GTIN and supplier info. */
    // Helper method to create a complete trade item event payload used in trade item tests
    private Map<String, Object> tradeItemMessage() {
        // Create a mutable HashMap for the trade item message
        Map<String, Object> msg = new HashMap<>();
        // Set the event ID used for Level 2 deduplication
        msg.put("eventId", "evt_trade_001");
        // Set the GTIN (Global Trade Item Number) used as the business key for Level 3 deduplication
        msg.put("gtin", "12345678901234");
        // Set the SKU ID linking this trade item to an item
        msg.put("skuId", "SKU001");
        // Set the supplier ID for supplier association
        msg.put("supplierId", "SUP001");
        // Set the supplier name for display purposes
        msg.put("supplierName", "Test Supplier");
        // Set the minimum order quantity for this trade item
        msg.put("minOrderQuantity", 10);
        // Set the lead time in days for ordering this trade item
        msg.put("leadTimeDays", 7);
        // Set the source system identifier
        msg.put("sourceSystem", "test");
        // Return the fully populated trade item message map
        return msg;
    }

    /** Creates a valid supplier supply event message with supplier and warehouse data. */
    // Helper method to create a complete supplier supply event payload used in supplier supply tests
    private Map<String, Object> supplierSupplyMessage() {
        // Create a mutable HashMap for the supplier supply message
        Map<String, Object> msg = new HashMap<>();
        // Set the event ID used for Level 2 deduplication
        msg.put("eventId", "evt_sup_001");
        // Set the supplier ID used as the business key for Level 3 deduplication
        msg.put("supplierId", "SUP001");
        // Set the warehouse ID for location tracking
        msg.put("warehouseId", "WH001");
        // Set the warehouse name for display purposes
        msg.put("warehouseName", "Main Warehouse");
        // Set the SKU ID linking this supply to an item
        msg.put("skuId", "SKU001");
        // Set the available quantity in stock
        msg.put("availableQuantity", 100);
        // Set the reserved quantity (committed to orders but not shipped)
        msg.put("reservedQuantity", 20);
        // Set the on-order quantity (ordered from supplier, not yet received)
        msg.put("onOrderQuantity", 50);
        // Set the reorder point threshold for low stock alerting
        msg.put("reorderPoint", 25);
        // Set the source system identifier
        msg.put("sourceSystem", "test");
        // Return the fully populated supplier supply message map
        return msg;
    }

    /** Creates a valid shipment event message with tracking and carrier data. */
    // Helper method to create a complete shipment event payload used in shipment tests
    private Map<String, Object> shipmentMessage() {
        // Create a mutable HashMap for the shipment message
        Map<String, Object> msg = new HashMap<>();
        // Set the event ID used for Level 2 deduplication
        msg.put("eventId", "evt_ship_001");
        // Set the tracking number used as the business key for Level 3 deduplication
        msg.put("trackingNumber", "TRACK123");
        // Set the order ID linking this shipment to a customer order
        msg.put("orderId", "ORD001");
        // Set the carrier name (e.g., FedEx, UPS, DHL)
        msg.put("carrier", "FedEx");
        // Set the current shipment status (e.g., IN_TRANSIT, DELIVERED)
        msg.put("shipmentStatus", "IN_TRANSIT");
        // Set the origin location of the shipment
        msg.put("originLocation", "New York");
        // Set the destination location of the shipment
        msg.put("destinationLocation", "LA");
        // Set the current location of the shipment for tracking
        msg.put("currentLocation", "Chicago");
        // Set the source system identifier
        msg.put("sourceSystem", "test");
        // Return the fully populated shipment message map
        return msg;
    }

    /**
     * Calculates SHA-256 checksum for test data, mirroring the service's calculateChecksum().
     * Used to set up Level 4 dedup test scenarios.
     */
    // Helper method to compute the same SHA-256 hash that the production code uses for deduplication
    private String calculateTestChecksum(Map<String, Object> data) throws Exception {
        // Serialize the map to a JSON string using the same ObjectMapper instance the service uses
        String json = objectMapper.writeValueAsString(data);
        // Get an instance of the SHA-256 message digest algorithm
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        // Compute the SHA-256 hash of the JSON bytes (using UTF-8 encoding for consistency)
        byte[] hash = digest.digest(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // Build a hex string representation of the hash bytes
        StringBuilder sb = new StringBuilder();
        // Iterate over each byte in the hash array
        for (byte b : hash) {
            // Convert each byte to a 2-character lowercase hex string and append it
            sb.append(String.format("%02x", b));
        }
        // Return the complete 64-character hex string (SHA-256 produces 256 bits = 32 bytes = 64 hex chars)
        return sb.toString();
    }
}
