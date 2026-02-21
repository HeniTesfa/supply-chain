// Package declaration: places this test class in the consumer service's service package
package com.supplychain.consumer.service;

// Import ItemEntity for verifying that item events are saved with the correct entity type
import com.supplychain.consumer.entity.ItemEntity;
// Import ShipmentEntity for verifying that shipment events are saved with the correct entity type
import com.supplychain.consumer.entity.ShipmentEntity;
// Import TradeItemEntity for verifying that trade item events are saved with the correct entity type
import com.supplychain.consumer.entity.TradeItemEntity;
// Import all repository interfaces: ItemRepository, TradeItemRepository, SupplierSupplyRepository, ShipmentRepository, ErrorLogRepository
import com.supplychain.consumer.repository.*;
// Import ProducerRecord for creating Kafka messages (not directly used but available for advanced tests)
import org.apache.kafka.clients.producer.ProducerRecord;
// Import @BeforeEach for setup methods that run before each individual test
import org.junit.jupiter.api.BeforeEach;
// Import @Test annotation to mark methods as JUnit 5 test cases
import org.junit.jupiter.api.Test;
// Import ArgumentCaptor to capture arguments passed to mock methods for inspection
import org.mockito.ArgumentCaptor;
// Import @Autowired for Spring dependency injection of beans from the application context
import org.springframework.beans.factory.annotation.Autowired;
// Import @SpringBootTest to load the full Spring application context for integration testing
import org.springframework.boot.test.context.SpringBootTest;
// Import @MockBean to replace real beans with Mockito mocks in the Spring context
import org.springframework.boot.test.mock.mockito.MockBean;
// Import KafkaTemplate for publishing test messages to embedded Kafka topics
import org.springframework.kafka.core.KafkaTemplate;
// Import @EmbeddedKafka to start an in-memory Kafka broker for integration testing
import org.springframework.kafka.test.context.EmbeddedKafka;
// Import @ActiveProfiles to activate the "test" Spring profile for test-specific configuration
import org.springframework.test.context.ActiveProfiles;

// Import HashMap for creating mutable event message maps
import java.util.HashMap;
// Import Map interface for event message payloads
import java.util.Map;
// Import Optional for wrapping repository lookup results
import java.util.Optional;
// Import TimeUnit for readable sleep durations while waiting for asynchronous Kafka processing
import java.util.concurrent.TimeUnit;

// Import AssertJ's assertThat for fluent assertions
import static org.assertj.core.api.Assertions.assertThat;
// Import Mockito's any() matcher for matching any argument type
import static org.mockito.ArgumentMatchers.any;
// Import Mockito's anyString() matcher for matching any String argument
import static org.mockito.ArgumentMatchers.anyString;
// Import all static Mockito methods: mock(), when(), verify(), atMost(), timeout(), etc.
import static org.mockito.Mockito.*;

// @SpringBootTest loads the full Spring Boot application context including all beans and configuration
@SpringBootTest
// @EmbeddedKafka starts an in-memory Kafka broker for this test class
@EmbeddedKafka(
        // Configure the embedded Kafka broker with 1 partition per topic
        partitions = 1,
        // Pre-create the 4 supply chain event topics that the consumer listens to
        topics = {
                "supply-chain.item.events",
                "supply-chain.trade-item.events",
                "supply-chain.supplier-supply.events",
                "supply-chain.shipment-tracking.events"
        },
        // Bind the broker to localhost on a random available port (port 0 = OS-assigned)
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
// Activate the "test" Spring profile to load test-specific application-test.yml configuration
@ActiveProfiles("test")
// Declare the integration test class with package-private visibility
class KafkaConsumerIntegrationTest {

    // Inject the KafkaTemplate bean configured by Spring Boot for publishing test messages
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Replace the real ItemRepository with a Mockito mock in the Spring context
    @MockBean
    private ItemRepository itemRepository;

    // Replace the real TradeItemRepository with a Mockito mock in the Spring context
    @MockBean
    private TradeItemRepository tradeItemRepository;

    // Replace the real SupplierSupplyRepository with a Mockito mock in the Spring context
    @MockBean
    private SupplierSupplyRepository supplierSupplyRepository;

    // Replace the real ShipmentRepository with a Mockito mock in the Spring context
    @MockBean
    private ShipmentRepository shipmentRepository;

    // Replace the real ErrorLogRepository with a Mockito mock in the Spring context
    @MockBean
    private ErrorLogRepository errorLogRepository;

    // Replace the real LoaderServiceClient with a Mockito mock to prevent actual HTTP calls
    @MockBean
    private LoaderServiceClient loaderServiceClient;

    // @BeforeEach runs before each test to set up default mock behavior
    @BeforeEach
    // Setup method: configures all repository mocks to simulate "no duplicates exist" by default
    void setUp() {
        // Default: item event ID lookup returns empty (no duplicate) for any event ID
        when(itemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Default: item SKU lookup returns empty (no duplicate business key)
        when(itemRepository.findBySkuId(anyString())).thenReturn(Optional.empty());
        // Default: item save returns the argument itself (echoes back the saved entity)
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Default: trade item event ID lookup returns empty (no duplicate)
        when(tradeItemRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Default: trade item GTIN lookup returns empty (no duplicate business key)
        when(tradeItemRepository.findByGtin(anyString())).thenReturn(Optional.empty());
        // Default: trade item save returns the argument itself
        when(tradeItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Default: shipment event ID lookup returns empty (no duplicate)
        when(shipmentRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Default: shipment tracking number lookup returns empty (no duplicate business key)
        when(shipmentRepository.findByTrackingNumber(anyString())).thenReturn(Optional.empty());
        // Default: shipment save returns the argument itself
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Default: supplier supply event ID lookup returns empty (no duplicate)
        when(supplierSupplyRepository.findByEventId(anyString())).thenReturn(Optional.empty());
        // Default: supplier supply supplier ID lookup returns empty (no duplicate business key)
        when(supplierSupplyRepository.findBySupplierId(anyString())).thenReturn(Optional.empty());
        // Default: supplier supply save returns the argument itself
        when(supplierSupplyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: publishes an item event to the embedded Kafka topic and verifies it is saved
    void consumeItemEvent_fromKafka_savesToRepository() throws Exception {
        // Create a test item event payload map
        Map<String, Object> event = new HashMap<>();
        // Set the event ID for this integration test item event
        event.put("eventId", "evt_item_int_001");
        // Set the SKU ID business key
        event.put("skuId", "SKU-INT-001");
        // Set the item name
        event.put("itemName", "Integration Laptop");
        // Set the category
        event.put("category", "Electronics");
        // Set the price
        event.put("price", 499.99);

        // Publish the event to the item topic using KafkaTemplate; .get() blocks until send completes
        kafkaTemplate.send("supply-chain.item.events", "evt_item_int_001", event).get();

        // Wait 3 seconds for the asynchronous Kafka consumer to pick up and process the message
        TimeUnit.SECONDS.sleep(3);

        // Verify that itemRepository.save() was called with an ItemEntity within a 5-second timeout
        // timeout(5000) allows the assertion to wait up to 5 seconds for the async consumer to complete
        verify(itemRepository, timeout(5000)).save(any(ItemEntity.class));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: publishes a trade item event and verifies it is saved to the trade item repository
    void consumeTradeItemEvent_fromKafka_savesToRepository() throws Exception {
        // Create a test trade item event payload map
        Map<String, Object> event = new HashMap<>();
        // Set the event ID for this integration test trade item event
        event.put("eventId", "evt_trade_int_001");
        // Set the GTIN (Global Trade Item Number) business key
        event.put("gtin", "12345678901234");
        // Set the SKU ID
        event.put("skuId", "SKU-INT-002");
        // Set the supplier ID
        event.put("supplierId", "SUP-INT-001");

        // Publish the event to the trade-item topic; .get() blocks until the broker acknowledges
        kafkaTemplate.send("supply-chain.trade-item.events", "evt_trade_int_001", event).get();

        // Wait 3 seconds for the Kafka consumer to process the message asynchronously
        TimeUnit.SECONDS.sleep(3);

        // Verify that tradeItemRepository.save() was called within a 5-second timeout
        verify(tradeItemRepository, timeout(5000)).save(any(TradeItemEntity.class));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: publishes a shipment event and verifies it is saved to the shipment repository
    void consumeShipmentEvent_fromKafka_savesToRepository() throws Exception {
        // Create a test shipment event payload map
        Map<String, Object> event = new HashMap<>();
        // Set the event ID for this integration test shipment event
        event.put("eventId", "evt_ship_int_001");
        // Set the tracking number business key
        event.put("trackingNumber", "TRACK-INT-001");
        // Set the carrier name
        event.put("carrier", "UPS");
        // Set the shipment status
        event.put("shipmentStatus", "DELIVERED");

        // Publish the event to the shipment-tracking topic; .get() blocks until broker acknowledges
        kafkaTemplate.send("supply-chain.shipment-tracking.events", "evt_ship_int_001", event).get();

        // Wait 3 seconds for the Kafka consumer to process the message asynchronously
        TimeUnit.SECONDS.sleep(3);

        // Verify that shipmentRepository.save() was called within a 5-second timeout
        verify(shipmentRepository, timeout(5000)).save(any(ShipmentEntity.class));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: publishes the same item event twice and verifies only the first is saved
    void consumeDuplicateItemEvent_skipsSecondSave() throws Exception {
        // Create a test item event that will be sent twice to test deduplication
        Map<String, Object> event = new HashMap<>();
        // Set the event ID that will be duplicated
        event.put("eventId", "evt_item_dup_001");
        // Set the SKU ID business key
        event.put("skuId", "SKU-DUP-001");
        // Set the item name
        event.put("itemName", "Duplicate Test");

        // First send: publish the event for the first time (should be saved)
        kafkaTemplate.send("supply-chain.item.events", "evt_item_dup_001", event).get();

        // Wait 3 seconds for the first event to be consumed and processed
        TimeUnit.SECONDS.sleep(3);

        // Now simulate that the event ID already exists in the database for the second message
        // This changes the Level 2 dedup behavior to return a found entity
        when(itemRepository.findByEventId("evt_item_dup_001")).thenReturn(Optional.of(new ItemEntity()));

        // Second send: publish the same event again (should be skipped by dedup)
        kafkaTemplate.send("supply-chain.item.events", "evt_item_dup_001", event).get();

        // Wait 3 seconds for the second event to be consumed (and skipped)
        TimeUnit.SECONDS.sleep(3);

        // Assert: save should have been called at most 1 time (only for the first non-duplicate event)
        verify(itemRepository, atMost(1)).save(any(ItemEntity.class));
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: verifies that after saving an event, it is forwarded to the loader service
    void consumeEvent_afterSave_forwardsToLoader() throws Exception {
        // Create a test item event payload map
        Map<String, Object> event = new HashMap<>();
        // Set the event ID for this forwarding test
        event.put("eventId", "evt_item_fwd_001");
        // Set the SKU ID business key
        event.put("skuId", "SKU-FWD-001");
        // Set the item name
        event.put("itemName", "Forward Test");

        // Publish the event to the item topic; .get() blocks until broker acknowledges
        kafkaTemplate.send("supply-chain.item.events", "evt_item_fwd_001", event).get();

        // Wait 3 seconds for the consumer to process the message
        TimeUnit.SECONDS.sleep(3);

        // Verify that the loader service client's forwardToLoader() was called within a 5-second timeout
        verify(loaderServiceClient, timeout(5000)).forwardToLoader(any());
    }
}
