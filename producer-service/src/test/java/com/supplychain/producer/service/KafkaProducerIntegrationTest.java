// Package declaration: places this test class in the producer service's service package
package com.supplychain.producer.service;

// Import the ProducedEvent entity that records published events in MongoDB
import com.supplychain.producer.entity.ProducedEvent;
// Import the ProducedEventRepository interface for mocking database operations
import com.supplychain.producer.repository.ProducedEventRepository;
// Import Kafka Consumer for reading messages from topics in verification
import org.apache.kafka.clients.consumer.Consumer;
// Import ConsumerConfig for setting Kafka consumer configuration properties
import org.apache.kafka.clients.consumer.ConsumerConfig;
// Import ConsumerRecords which holds the batch of records fetched from Kafka
import org.apache.kafka.clients.consumer.ConsumerRecords;
// Import StringDeserializer for deserializing Kafka message keys from bytes to Strings
import org.apache.kafka.common.serialization.StringDeserializer;
// Import @AfterEach for teardown methods that run after each individual test
import org.junit.jupiter.api.AfterEach;
// Import @BeforeEach for setup methods that run before each individual test
import org.junit.jupiter.api.BeforeEach;
// Import @Test annotation to mark methods as JUnit 5 test cases
import org.junit.jupiter.api.Test;
// Import @Autowired for Spring dependency injection
import org.springframework.beans.factory.annotation.Autowired;
// Import @SpringBootTest to load the full Spring application context for integration testing
import org.springframework.boot.test.context.SpringBootTest;
// Import @MockBean to replace real beans with Mockito mocks in the Spring context
import org.springframework.boot.test.mock.mockito.MockBean;
// Import DefaultKafkaConsumerFactory for creating Kafka consumer instances in tests
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
// Import JsonDeserializer for deserializing Kafka message values from JSON bytes to Objects
import org.springframework.kafka.support.serializer.JsonDeserializer;
// Import EmbeddedKafkaBroker for accessing the embedded Kafka broker instance
import org.springframework.kafka.test.EmbeddedKafkaBroker;
// Import @EmbeddedKafka to start an in-memory Kafka broker for integration testing
import org.springframework.kafka.test.context.EmbeddedKafka;
// Import KafkaTestUtils for creating consumer properties and reading records from topics
import org.springframework.kafka.test.utils.KafkaTestUtils;
// Import @ActiveProfiles to activate the "test" Spring profile
import org.springframework.test.context.ActiveProfiles;

// Import LocalDateTime for building ProducedEvent entities with timestamps
import java.time.LocalDateTime;
// Import HashMap for creating mutable event data maps
import java.util.HashMap;
// Import Map interface for event payloads and response objects
import java.util.Map;
// Import Optional for wrapping repository lookup results
import java.util.Optional;

// Import AssertJ's assertThat for fluent assertions
import static org.assertj.core.api.Assertions.assertThat;
// Import Mockito's any() matcher for matching any argument type
import static org.mockito.ArgumentMatchers.any;
// Import Mockito's anyString() matcher for matching any String argument
import static org.mockito.ArgumentMatchers.anyString;
// Import Mockito's when() to stub mock method return values
import static org.mockito.Mockito.when;

// @SpringBootTest loads the full Spring Boot application context including auto-configured KafkaTemplate
@SpringBootTest
// @EmbeddedKafka starts an in-memory Kafka broker for integration tests
@EmbeddedKafka(
        // Configure 1 partition per topic for deterministic ordering in tests
        partitions = 1,
        // Pre-create the 4 supply chain event topics
        topics = {
                "supply-chain.item.events",
                "supply-chain.trade-item.events",
                "supply-chain.shipment-tracking.events",
                "supply-chain.supplier-supply.events"
        },
        // Bind the broker to localhost on a random available port
        brokerProperties = {"listeners=PLAINTEXT://localhost:0"}
)
// Activate the "test" Spring profile for test-specific application configuration
@ActiveProfiles("test")
// Declare the integration test class with package-private visibility
class KafkaProducerIntegrationTest {

    // Inject the embedded Kafka broker instance for test consumer setup
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    // Inject the real KafkaProducerService bean (wired to the embedded Kafka broker)
    @Autowired
    private KafkaProducerService producerService;

    // Replace the real ProducedEventRepository with a mock to avoid needing a real MongoDB
    @MockBean
    private ProducedEventRepository producedEventRepository;

    // Test consumer instance used to verify messages were actually published to Kafka topics
    private Consumer<String, Object> testConsumer;

    // @BeforeEach runs before each test to set up mocks and create a fresh test consumer
    @BeforeEach
    // Setup method: configures repository mocks and creates a Kafka consumer for verification
    void setUp() {
        // Stub: idempotency key lookup returns empty by default (no duplicates)
        when(producedEventRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        // Stub: content hash lookup returns empty by default (no content duplicates)
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());
        // Stub: save returns the argument itself (echoes back the saved entity)
        when(producedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Get consumer properties configured for the embedded broker, with group "test-group" and auto-commit
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        // Set the auto offset reset to "earliest" so the test consumer reads from the beginning of each topic
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Configure the key deserializer to StringDeserializer (keys are String event IDs)
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Configure the value deserializer to JsonDeserializer for deserializing JSON event payloads
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Trust all packages for JSON deserialization (required for test to deserialize Map objects)
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Create a Kafka consumer factory with the configured properties
        DefaultKafkaConsumerFactory<String, Object> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        // Create a test consumer instance that will be used to read messages from topics
        testConsumer = consumerFactory.createConsumer();
    }

    // @AfterEach runs after each test to clean up resources
    @AfterEach
    // Teardown method: closes the test consumer to release Kafka resources
    void tearDown() {
        // Only close if the consumer was successfully created
        if (testConsumer != null) {
            // Close the consumer to release its partition assignments and network connections
            testConsumer.close();
        }
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: publishes an item event and verifies it appears on the Kafka topic
    void publishEvent_item_messageAppearsOnTopic() {
        // Subscribe the test consumer to the item events topic on the embedded broker
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(testConsumer, "supply-chain.item.events");

        // Create a test item event data map
        Map<String, Object> eventData = new HashMap<>();
        // Set the SKU ID for the test item
        eventData.put("skuId", "SKU-INT-001");
        // Set the item name for the test item
        eventData.put("itemName", "Integration Test Laptop");

        // Act: publish the item event through the real KafkaProducerService
        Map<String, Object> result = producerService.publishEvent("item", eventData, null);

        // Assert: the publish response indicates success
        assertThat(result.get("success")).isEqualTo(true);
        // Assert: the event was routed to the correct Kafka topic
        assertThat(result.get("topic")).isEqualTo("supply-chain.item.events");

        // Read records from the topic using the test consumer to verify the message was published
        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(testConsumer);
        // Assert: at least 1 record was published to the topic
        assertThat(records.count()).isGreaterThanOrEqualTo(1);
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: publishes a shipment event and verifies it appears on the shipment topic
    void publishEvent_shipment_messageAppearsOnShipmentTopic() {
        // Subscribe the test consumer to the shipment-tracking events topic
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(testConsumer, "supply-chain.shipment-tracking.events");

        // Create a test shipment event data map
        Map<String, Object> eventData = new HashMap<>();
        // Set the tracking number for the test shipment
        eventData.put("trackingNumber", "TRACK-INT-001");
        // Set the carrier for the test shipment
        eventData.put("carrier", "UPS");

        // Act: publish the shipment event through the real KafkaProducerService
        Map<String, Object> result = producerService.publishEvent("shipment", eventData, null);

        // Assert: the publish response indicates success
        assertThat(result.get("success")).isEqualTo(true);
        // Assert: the event was routed to the shipment-tracking topic
        assertThat(result.get("topic")).isEqualTo("supply-chain.shipment-tracking.events");

        // Read records from the topic using the test consumer
        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(testConsumer);
        // Assert: at least 1 record was published to the shipment topic
        assertThat(records.count()).isGreaterThanOrEqualTo(1);
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: verifies that a duplicate idempotency key prevents re-publishing to Kafka
    void publishEvent_idempotencyKeyDuplicate_preventsPublish() {
        // Build a ProducedEvent entity representing a previously published event
        ProducedEvent existing = ProducedEvent.builder()
                // Set the original event ID from the first publication
                .eventId("evt_item_existing")
                // Set the topic the event was originally published to
                .topic("supply-chain.item.events")
                // Set the Kafka partition
                .partition(0)
                // Set the Kafka offset
                .offset(0L)
                // Set the publication timestamp
                .publishedAt(LocalDateTime.now())
                // Build the ProducedEvent object
                .build();
        // Stub: when looking up idempotency key "dup-key", return the existing event
        when(producedEventRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        // Create a test event data map
        Map<String, Object> eventData = new HashMap<>();
        // Set the SKU ID
        eventData.put("skuId", "SKU-DUP");

        // Act: attempt to publish with the duplicate idempotency key
        Map<String, Object> result = producerService.publishEvent("item", eventData, "dup-key");

        // Assert: the response indicates this is a duplicate
        assertThat(result.get("duplicate")).isEqualTo(true);
        // Assert: the original event ID is returned
        assertThat(result.get("eventId")).isEqualTo("evt_item_existing");
    }

    // @Test marks this method as a JUnit 5 test case
    @Test
    // Integration test: verifies that after publishing, the ProducedEvent is saved to the repository
    void publishEvent_success_savesProducedEventToRepo() {
        // Stub: content hash lookup returns empty (no content duplicate)
        when(producedEventRepository.findByContentHash(anyString())).thenReturn(Optional.empty());

        // Create a test event data map
        Map<String, Object> eventData = new HashMap<>();
        // Set the SKU ID for the test item
        eventData.put("skuId", "SKU-SAVE-001");
        // Set the item name for the test item
        eventData.put("itemName", "Save Test");

        // Act: publish the item event with idempotency key "save-key"
        producerService.publishEvent("item", eventData, "save-key");

        // Assert: verify that the repository save() was called with a ProducedEvent entity
        // Uses the fully qualified Mockito.verify() since static import is not available
        org.mockito.Mockito.verify(producedEventRepository).save(any(ProducedEvent.class));
    }
}
