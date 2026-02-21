// Package declaration: places this test class in the producer service's service package
package com.supplychain.producer.service;

// Import Jackson's ObjectMapper for JSON serialization used in content hash computation
import com.fasterxml.jackson.databind.ObjectMapper;
// Import the ProducedEvent entity that records published events in MongoDB
import com.supplychain.producer.entity.ProducedEvent;
// Import the ProducedEventRepository interface for mocking database lookups and saves
import com.supplychain.producer.repository.ProducedEventRepository;
// Import RecordMetadata which contains the partition, offset, and topic of a sent Kafka record
import org.apache.kafka.clients.producer.RecordMetadata;
// Import TopicPartition which represents a specific partition of a Kafka topic
import org.apache.kafka.common.TopicPartition;
// Import @BeforeEach for setup methods that run before each individual test
import org.junit.jupiter.api.BeforeEach;
// Import @Test annotation to mark methods as JUnit 5 test cases
import org.junit.jupiter.api.Test;
// Import @ExtendWith to integrate JUnit 5 with the Mockito extension
import org.junit.jupiter.api.extension.ExtendWith;
// Import ArgumentCaptor to capture arguments passed to mocked methods for inspection
import org.mockito.ArgumentCaptor;
// Import @InjectMocks to create an instance of the service with all @Mock/@Spy fields injected
import org.mockito.InjectMocks;
// Import @Mock to create mock instances of dependencies
import org.mockito.Mock;
// Import @Spy to create a partial mock that uses real implementation by default
import org.mockito.Spy;
// Import MockitoExtension to enable Mockito annotations in JUnit 5 tests
import org.mockito.junit.jupiter.MockitoExtension;
// Import KafkaTemplate for publishing messages to Kafka topics (mocked in this test)
import org.springframework.kafka.core.KafkaTemplate;
// Import SendResult which wraps the result of a Kafka send operation
import org.springframework.kafka.support.SendResult;

// Import LocalDateTime for building ProducedEvent entities with timestamp fields
import java.time.LocalDateTime;
// Import HashMap for creating mutable event data maps
import java.util.HashMap;
// Import Map interface for event payloads and response objects
import java.util.Map;
// Import Optional for wrapping repository lookup results
import java.util.Optional;
// Import CompletableFuture for simulating asynchronous Kafka send results
import java.util.concurrent.CompletableFuture;

// Import AssertJ's assertThat for fluent, readable assertions
import static org.assertj.core.api.Assertions.assertThat;
// Import AssertJ's assertThatThrownBy for asserting that code throws specific exceptions
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// Import Mockito's any() matcher for matching any argument type
import static org.mockito.ArgumentMatchers.any;
// Import Mockito's anyString() matcher for matching any String argument
import static org.mockito.ArgumentMatchers.anyString;
// Import all static Mockito methods: mock(), when(), verify(), never(), lenient(), eq(), etc.
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KafkaProducerService}.
 *
 * Tests the producer-side deduplication and Kafka publishing logic including:
 * - Idempotency key dedup (prevents duplicate submissions from the same client)
 * - Content hash dedup (prevents publishing identical event payloads)
 * - Kafka topic routing for each event type (item, trade-item, shipment, supplier-supply)
 * - Event ID generation with type prefix
 * - ProducedEvent persistence to MongoDB after successful publish
 * - Error handling when Kafka publish fails
 *
 * Dependencies (KafkaTemplate, ProducedEventRepository, ObjectMapper) are mocked.
 */
// @ExtendWith(MockitoExtension.class) integrates Mockito with JUnit 5 for annotation-based mocking
@ExtendWith(MockitoExtension.class)
// Declare the test class with package-private visibility
class KafkaProducerServiceTest {

    // Mock the KafkaTemplate dependency for publishing messages to Kafka topics
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    // Mock the ProducedEventRepository for database lookups (idempotency key, content hash) and saves
    @Mock private ProducedEventRepository producedEventRepository;
    // @Spy creates a partial mock of ObjectMapper: real JSON serialization is used for hash computation
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    // @InjectMocks creates an instance of KafkaProducerService and injects all @Mock/@Spy fields
    @InjectMocks
    // The service under test: handles event publishing with deduplication
    private KafkaProducerService service;

    // @BeforeEach runs before each test to set up default mock behavior
    @BeforeEach
    // Setup method: configures repository mocks to return empty by default (no duplicates)
    void setUp() {
        // lenient(): allows this stubbing even if not used by every test (avoids UnnecessaryStubbingException)
        // Default: idempotency key lookup returns empty (no previous submission with this key)
        lenient().when(producedEventRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        // Default: content hash lookup returns empty (no previously published event with this hash)
        lenient().when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());
    }

    // ==================== Idempotency Key Dedup ====================

    /**
     * Verifies that when the same idempotency key is resubmitted, the service returns
     * the original event details with duplicate=true and does NOT publish to Kafka again.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: reusing an idempotency key returns the original event details without re-publishing
    void publishEvent_duplicateIdempotencyKey_returnsDuplicate() {
        // Build a ProducedEvent entity representing the previously published event with this key
        ProducedEvent existing = ProducedEvent.builder()
                // Set the original event ID from the first publication
                .eventId("evt_item_abc")
                // Set the topic the event was originally published to
                .topic("supply-chain.item.events")
                // Set the partition where the event was stored
                .partition(0)
                // Set the offset within the partition
                .offset(42L)
                // Set the timestamp of the original publication
                .publishedAt(LocalDateTime.now())
                // Build the ProducedEvent object
                .build();
        // Stub: when looking up idempotency key "key-123", return the existing event
        when(producedEventRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

        // Act: attempt to publish with the same idempotency key "key-123"
        Map<String, Object> result = service.publishEvent("item", itemData(), "key-123");

        // Assert: the response indicates this is a duplicate
        assertThat(result.get("duplicate")).isEqualTo(true);
        // Assert: the response returns the original event ID
        assertThat(result.get("eventId")).isEqualTo("evt_item_abc");
        // Assert: Kafka send was NEVER called because the duplicate was caught before publishing
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    /**
     * Verifies that when no idempotency key is provided (null),
     * the idempotency check is skipped entirely and the event is published.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: null idempotency key skips the idempotency check and proceeds to publish
    void publishEvent_nullIdempotencyKey_skipsIdempotencyCheck() {
        // Set up a successful Kafka mock for the item topic
        setupKafkaSuccess("supply-chain.item.events");
        // Stub: content hash lookup returns empty (no content duplicate)
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Act: publish with null idempotency key
        Map<String, Object> result = service.publishEvent("item", itemData(), null);

        // Assert: the event was published successfully (not a duplicate)
        assertThat(result.get("duplicate")).isEqualTo(false);
        // Assert: findByIdempotencyKey was NEVER called because the key was null
        verify(producedEventRepository, never()).findByIdempotencyKey(anyString());
    }

    // ==================== Content Hash Dedup ====================

    /**
     * Verifies that when an identical event payload has already been published
     * (matching SHA-256 hash), the service returns duplicate=true without re-publishing.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when the content hash matches a previously published event, return duplicate
    void publishEvent_duplicateContentHash_returnsDuplicate() {
        // Build a ProducedEvent entity representing a previously published event with the same content
        ProducedEvent existing = ProducedEvent.builder()
                // Set the original event ID
                .eventId("evt_item_old")
                // Set the topic the event was originally published to
                .topic("supply-chain.item.events")
                // Set the Kafka partition
                .partition(1)
                // Set the Kafka offset
                .offset(99L)
                // Set the publication timestamp
                .publishedAt(LocalDateTime.now())
                // Build the ProducedEvent object
                .build();
        // Stub: any content hash lookup returns the existing event (content hash match)
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.of(existing));

        // Act: attempt to publish with identical content (hash will match)
        Map<String, Object> result = service.publishEvent("item", itemData(), null);

        // Assert: the response indicates this is a content duplicate
        assertThat(result.get("duplicate")).isEqualTo(true);
        // Assert: Kafka send was NEVER called because the content hash matched
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    // ==================== Topic Routing ====================

    /** Verifies that "item" events are published to the supply-chain.item.events topic. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: "item" event type routes to the correct Kafka topic
    void publishEvent_itemType_sendsToItemTopic() {
        // Set up a successful Kafka mock for the item topic
        setupKafkaSuccess("supply-chain.item.events");
        // Stub: content hash lookup returns empty (no content duplicate)
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Act: publish an event with type "item"
        service.publishEvent("item", itemData(), null);

        // Assert: the message was sent to the "supply-chain.item.events" topic
        verify(kafkaTemplate).send(eq("supply-chain.item.events"), anyString(), any());
    }

    /** Verifies that "trade-item" events are published to the supply-chain.trade-item.events topic. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: "trade-item" event type routes to the correct Kafka topic
    void publishEvent_tradeItemType_sendsToTradeItemTopic() {
        // Set up a successful Kafka mock for the trade-item topic
        setupKafkaSuccess("supply-chain.trade-item.events");
        // Stub: content hash lookup returns empty
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Act: publish an event with type "trade-item"
        service.publishEvent("trade-item", itemData(), null);

        // Assert: the message was sent to the "supply-chain.trade-item.events" topic
        verify(kafkaTemplate).send(eq("supply-chain.trade-item.events"), anyString(), any());
    }

    /** Verifies that "shipment" events are published to the supply-chain.shipment-tracking.events topic. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: "shipment" event type routes to the correct Kafka topic
    void publishEvent_shipmentType_sendsToShipmentTopic() {
        // Set up a successful Kafka mock for the shipment-tracking topic
        setupKafkaSuccess("supply-chain.shipment-tracking.events");
        // Stub: content hash lookup returns empty
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Act: publish an event with type "shipment"
        service.publishEvent("shipment", itemData(), null);

        // Assert: the message was sent to the "supply-chain.shipment-tracking.events" topic
        verify(kafkaTemplate).send(eq("supply-chain.shipment-tracking.events"), anyString(), any());
    }

    /** Verifies that "supplier-supply" events are published to the supplier-supply topic. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: "supplier-supply" event type routes to the correct Kafka topic
    void publishEvent_supplierSupplyType_sendsToSupplierTopic() {
        // Set up a successful Kafka mock for the supplier-supply topic
        setupKafkaSuccess("supply-chain.supplier-supply.events");
        // Stub: content hash lookup returns empty
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Act: publish an event with type "supplier-supply"
        service.publishEvent("supplier-supply", itemData(), null);

        // Assert: the message was sent to the "supply-chain.supplier-supply.events" topic
        verify(kafkaTemplate).send(eq("supply-chain.supplier-supply.events"), anyString(), any());
    }

    /** Verifies that an unknown event type throws a RuntimeException. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: an unrecognized event type should throw a RuntimeException
    void publishEvent_unknownType_throws() {
        // Stub: content hash lookup returns empty (event passes content hash check)
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Assert: publishing with an unknown type throws a RuntimeException
        assertThatThrownBy(() -> service.publishEvent("unknown", itemData(), null))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== Event ID Generation ====================

    /**
     * Verifies that generated event IDs follow the format "evt_{type}_{uuid}".
     * The event ID is injected into the event data before publishing.
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: generated event IDs start with "evt_item_" for item events
    void publishEvent_generatesEventIdWithTypePrefix() {
        // Set up a successful Kafka mock for the item topic
        setupKafkaSuccess("supply-chain.item.events");
        // Stub: content hash lookup returns empty
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Create an ArgumentCaptor to capture the event data map sent to Kafka
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        // Act: publish an item event
        service.publishEvent("item", itemData(), null);

        // Verify that kafkaTemplate.send() was called and capture the third argument (event data)
        verify(kafkaTemplate).send(anyString(), anyString(), dataCaptor.capture());
        // Retrieve the captured event data
        Map<String, Object> sentData = dataCaptor.getValue();
        // Assert: the generated event ID in the data starts with "evt_item_" prefix
        assertThat((String) sentData.get("eventId")).startsWith("evt_item_");
    }

    // ==================== MongoDB Persistence ====================

    /**
     * Verifies that after a successful Kafka publish, the event metadata is saved
     * to MongoDB with all required fields (eventId, topic, idempotency key, content hash).
     */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: after publishing, the ProducedEvent entity is saved to MongoDB with correct fields
    void publishEvent_success_savesProducedEvent() {
        // Set up a successful Kafka mock for the item topic
        setupKafkaSuccess("supply-chain.item.events");
        // Stub: content hash lookup returns empty
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Create an ArgumentCaptor to capture the ProducedEvent entity saved to the repository
        ArgumentCaptor<ProducedEvent> captor = ArgumentCaptor.forClass(ProducedEvent.class);

        // Act: publish an item event with idempotency key "my-key"
        service.publishEvent("item", itemData(), "my-key");

        // Verify that the repository save() was called and capture the saved entity
        verify(producedEventRepository).save(captor.capture());
        // Retrieve the captured ProducedEvent
        ProducedEvent saved = captor.getValue();
        // Assert: the event ID starts with "evt_item_" prefix
        assertThat(saved.getEventId()).startsWith("evt_item_");
        // Assert: the idempotency key was stored correctly
        assertThat(saved.getIdempotencyKey()).isEqualTo("my-key");
        // Assert: the Kafka topic was recorded correctly
        assertThat(saved.getTopic()).isEqualTo("supply-chain.item.events");
        // Assert: a content hash was computed and stored (SHA-256, should not be empty)
        assertThat(saved.getContentHash()).isNotEmpty();
        // Assert: the source system is set to "producer-service"
        assertThat(saved.getSourceSystem()).isEqualTo("producer-service");
    }

    // ==================== Kafka Failure Handling ====================

    /** Verifies that a Kafka broker failure results in a RuntimeException with a descriptive message. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: when Kafka send fails, the service throws a RuntimeException with error details
    void publishEvent_kafkaFails_throwsRuntimeException() {
        // Stub: content hash lookup returns empty (event passes dedup checks)
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());
        // Create a CompletableFuture that completes exceptionally to simulate a Kafka failure
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        // Complete the future with a RuntimeException to simulate "Kafka broker down"
        future.completeExceptionally(new RuntimeException("Kafka broker down"));
        // Stub: kafkaTemplate.send() returns the failed future
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // Assert: publishing should throw a RuntimeException with a descriptive message
        assertThatThrownBy(() -> service.publishEvent("item", itemData(), null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event");
    }

    // ==================== Response Structure ====================

    /** Verifies that a successful publish returns a response with all expected fields. */
    // @Test marks this method as a JUnit 5 test case
    @Test
    // Test method: successful publish returns a response map with success, duplicate, eventId, topic, etc.
    void publishEvent_success_returnsExpectedFields() {
        // Set up a successful Kafka mock for the item topic
        setupKafkaSuccess("supply-chain.item.events");
        // Stub: content hash lookup returns empty
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Act: publish an item event and capture the returned response map
        Map<String, Object> result = service.publishEvent("item", itemData(), null);

        // Assert: the response includes success=true
        assertThat(result.get("success")).isEqualTo(true);
        // Assert: the response includes duplicate=false (this is a new event)
        assertThat(result.get("duplicate")).isEqualTo(false);
        // Assert: the response contains all expected keys: eventId, topic, partition, offset
        assertThat(result).containsKeys("eventId", "topic", "partition", "offset");
    }

    // ==================== Test Data Helpers ====================

    /** Creates a simple item event payload for testing. */
    // Helper method to create a minimal item event data map
    private Map<String, Object> itemData() {
        // Create a mutable HashMap for the event data
        Map<String, Object> data = new HashMap<>();
        // Set the SKU ID field
        data.put("skuId", "SKU001");
        // Set the item name field
        data.put("itemName", "Laptop");
        // Set the price field
        data.put("price", 999.99);
        // Return the populated event data map
        return data;
    }

    /**
     * Sets up a mock KafkaTemplate that returns a successful SendResult.
     * Simulates a successful Kafka publish with partition 0 and offset 0.
     */
    // Suppress unchecked warnings for the generic SendResult mock creation
    @SuppressWarnings("unchecked")
    // Helper method to configure the mock KafkaTemplate to return a successful send result
    private void setupKafkaSuccess(String topic) {
        // Create a RecordMetadata object simulating a successful write to partition 0 at offset 0
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(topic, 0), 0, 0, 0L, 0, 0);
        // Create a mock SendResult and configure it to return the metadata
        SendResult<String, Object> sendResult = mock(SendResult.class);
        // Stub: getRecordMetadata() returns the simulated metadata with topic, partition, and offset info
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        // Create an already-completed CompletableFuture wrapping the successful SendResult
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        // Stub: kafkaTemplate.send() returns the completed future for any topic/key/value combination
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
    }
}
