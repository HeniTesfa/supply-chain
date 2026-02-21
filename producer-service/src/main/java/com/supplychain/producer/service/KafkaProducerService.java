// Declares this class belongs to the producer service's service layer package
package com.supplychain.producer.service;

// Import Jackson's ObjectMapper for serializing Java objects to JSON strings (used for content hashing)
import com.fasterxml.jackson.databind.ObjectMapper;
// Import the ProducedEvent entity class that represents a published event record stored in MongoDB
import com.supplychain.producer.entity.ProducedEvent;
// Import the repository interface for performing CRUD operations on ProducedEvent documents in MongoDB
import com.supplychain.producer.repository.ProducedEventRepository;
// Import the SLF4J Logger interface for structured logging throughout this service
import org.slf4j.Logger;
// Import the SLF4J LoggerFactory to create logger instances bound to specific classes
import org.slf4j.LoggerFactory;
// Import Spring's @Autowired annotation for automatic dependency injection of beans
import org.springframework.beans.factory.annotation.Autowired;
// Import Spring Kafka's KafkaTemplate which provides high-level operations for sending messages to Kafka topics
import org.springframework.kafka.core.KafkaTemplate;
// Import SendResult which wraps the result metadata (partition, offset, timestamp) returned after a Kafka send operation
import org.springframework.kafka.support.SendResult;
// Import Spring's @Service stereotype annotation to mark this class as a service-layer bean managed by the Spring container
import org.springframework.stereotype.Service;

// Import StandardCharsets for specifying UTF-8 encoding when converting strings to bytes for hashing
import java.nio.charset.StandardCharsets;
// Import MessageDigest which provides the SHA-256 cryptographic hash algorithm implementation
import java.security.MessageDigest;
// Import LocalDateTime for capturing timestamps of when events are published
import java.time.LocalDateTime;
// Import Map interface used for event data payloads and response objects throughout this service
import java.util.Map;
// Import UUID for generating universally unique identifiers used in event IDs and fallback hashes
import java.util.UUID;
// Import CompletableFuture which represents the asynchronous result of the Kafka send operation
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer Service
 *
 * Publishes events to Kafka with producer-side deduplication
 */
// @Service marks this class as a Spring service component, making it eligible for auto-detection via component scanning
// and registering it as a singleton bean in the Spring application context
@Service
// Declares the KafkaProducerService class which handles all Kafka publishing logic with built-in deduplication
public class KafkaProducerService {

    // Creates a static logger instance bound to this class for logging messages with the class name as the logger category
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    // @Autowired injects the KafkaTemplate bean configured by Spring Boot auto-configuration using properties from application.yml
    @Autowired
    // KafkaTemplate<String, Object> is the main Spring Kafka abstraction for sending messages; String is the key type, Object is the value type
    private KafkaTemplate<String, Object> kafkaTemplate;

    // @Autowired injects the ProducedEventRepository bean which Spring Data MongoDB auto-implements at runtime
    @Autowired
    // Repository for querying and saving ProducedEvent documents in the MongoDB "produced_events" collection
    private ProducedEventRepository producedEventRepository;

    // @Autowired injects Jackson's ObjectMapper bean (auto-configured by Spring Boot) for JSON serialization
    @Autowired
    // ObjectMapper converts Java objects to JSON strings, used here to serialize event data before computing SHA-256 hashes
    private ObjectMapper objectMapper;

    /**
     * Publish event to Kafka with deduplication
     *
     * @param eventType - item, trade-item, supplier, shipment
     * @param eventData - Event payload
     * @param idempotencyKey - Optional idempotency key
     * @return Map with event details
     */
    // Public method that publishes an event to Kafka after performing deduplication checks; returns a response map with publish metadata
    public Map<String, Object> publishEvent(String eventType, Map<String, Object> eventData,
                                            String idempotencyKey) {

        // Log an informational message indicating which event type is being published
        logger.info("Publishing {} event", eventType);

        // Calculate a SHA-256 hash of the event data to enable content-based deduplication (detects identical payloads)
        String contentHash = calculateHash(eventData);

        // Check if an idempotency key was provided by the client in the request header
        if (idempotencyKey != null) {
            // Query MongoDB to find any existing event that was previously published with the same idempotency key
            var existing = producedEventRepository.findByIdempotencyKey(idempotencyKey);
            // If a matching event is found, it means this is a duplicate submission
            if (existing.isPresent()) {
                // Log that a duplicate was detected based on the idempotency key, preventing re-publishing
                logger.info("DUPLICATE: Event already published with idempotency key: {}",
                    idempotencyKey);
                // Return the details of the originally published event with the duplicate flag set to true
                return buildResponse(existing.get(), true);
            }
        }

        // Query MongoDB to find any existing event with the same content hash (catches duplicates even without idempotency keys)
        var duplicateContent = producedEventRepository.findByContentHash(contentHash);
        // If a matching content hash is found, the same event data was already published before
        if (duplicateContent.isPresent()) {
            // Log that a duplicate was detected based on identical event content
            logger.info("DUPLICATE: Identical event already published");
            // Return the details of the originally published event with the duplicate flag set to true
            return buildResponse(duplicateContent.get(), true);
        }

        // Generate a unique event ID by combining the event type with a random UUID fragment
        String eventId = generateEventId(eventType);
        // Add the generated event ID to the event data map so it is included in the Kafka message payload
        eventData.put("eventId", eventId);
        // Add the event type to the event data map so downstream consumers know how to process this event
        eventData.put("eventType", eventType);
        // Add the current timestamp as an ISO-8601 string to the event data for tracking when it was created
        eventData.put("timestamp", LocalDateTime.now().toString());

        // Determine which Kafka topic to publish to based on the event type (e.g., "item" -> "supply-chain.item.events")
        String topic = getTopicForEventType(eventType);

        // Begin a try block to handle any exceptions that may occur during Kafka publishing
        try {
            // Send the event data to the determined Kafka topic using the event ID as the message key for partitioning
            // KafkaTemplate.send() returns a CompletableFuture that completes when Kafka acknowledges the message
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, eventId, eventData);

            // Block the current thread and wait for the Kafka send operation to complete, getting the result with metadata
            // This synchronous call ensures we have the partition and offset before saving to MongoDB
            SendResult<String, Object> result = future.get();

            // Build a ProducedEvent entity using the Lombok builder pattern to record this successful publish in MongoDB
            ProducedEvent producedEvent = ProducedEvent.builder()
                // Set the unique event ID that was generated for this event
                .eventId(eventId)
                // Set the client-provided idempotency key (may be null if not provided)
                .idempotencyKey(idempotencyKey)
                // Set the SHA-256 content hash for future content-based deduplication lookups
                .contentHash(contentHash)
                // Set the event type string (item, trade-item, supplier-supply, or shipment)
                .eventType(eventType)
                // Set the Kafka topic name where this event was published
                .topic(topic)
                // Set the Kafka partition number where this message was stored, extracted from the send result metadata
                .partition(result.getRecordMetadata().partition())
                // Set the Kafka offset within the partition, which uniquely identifies this message's position
                .offset(result.getRecordMetadata().offset())
                // Set the timestamp of when this event was published to Kafka
                .publishedAt(LocalDateTime.now())
                // Set the original event data payload for audit trail and potential replay scenarios
                .eventData(eventData)
                // Set the source system identifier to track which service produced this event
                .sourceSystem("producer-service")
                // Finalize the builder and create the ProducedEvent instance
                .build();

            // Persist the ProducedEvent document to MongoDB's "produced_events" collection for deduplication tracking
            producedEventRepository.save(producedEvent);

            // Log a success message with the event ID, topic, partition, and offset for operational monitoring
            logger.info("Event published successfully - eventId: {}, topic: {}, partition: {}, offset: {}",
                eventId, topic, result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

            // Return an immutable map containing the publish result details to the calling controller
            return Map.of(
                // Indicates the publish operation was successful
                "success", true,
                // The unique event ID assigned to this event
                "eventId", eventId,
                // The Kafka topic the event was published to
                "topic", topic,
                // The Kafka partition number where the message was stored
                "partition", result.getRecordMetadata().partition(),
                // The Kafka offset position of the message within the partition
                "offset", result.getRecordMetadata().offset(),
                // Indicates this is a new event, not a duplicate
                "duplicate", false
            );

        // Catch any exception that occurs during Kafka publishing or MongoDB saving
        } catch (Exception e) {
            // Log an error message with the exception details for troubleshooting
            logger.error("Failed to publish event: {}", e.getMessage(), e);
            // Throw a RuntimeException to propagate the failure up to the controller, which will return a 500 response
            throw new RuntimeException("Failed to publish event: " + e.getMessage());
        }
    }

    /**
     * Generate unique event ID
     */
    // Private helper method that generates a unique event ID by combining a prefix, event type, and a truncated UUID
    private String generateEventId(String eventType) {
        // Concatenate "evt_" prefix + event type + "_" + first 8 characters of a random UUID to create a compact unique ID
        return "evt_" + eventType + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get Kafka topic for event type
     */
    // Private helper method that maps an event type string to its corresponding Kafka topic name
    private String getTopicForEventType(String eventType) {
        // Switch on the lowercase version of the event type to perform case-insensitive matching
        switch (eventType.toLowerCase()) {
            // "item" events are published to the item events topic
            case "item":
                return "supply-chain.item.events";
            // "trade-item" events are published to the trade item events topic
            case "trade-item":
                return "supply-chain.trade-item.events";
            // "supplier-supply" events are published to the supplier supply events topic
            case "supplier-supply":
                return "supply-chain.supplier-supply.events";
            // "shipment" events are published to the shipment tracking events topic
            case "shipment":
                return "supply-chain.shipment-tracking.events";
            // If the event type does not match any known type, throw an exception to reject the invalid input
            default:
                throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }

    /**
     * Calculate content hash (SHA-256)
     */
    // Private helper method that computes a SHA-256 hash of the event data map for content-based deduplication
    private String calculateHash(Map<String, Object> data) {
        // Begin a try block to handle JSON serialization and hashing exceptions
        try {
            // Serialize the event data map to a JSON string using Jackson's ObjectMapper for consistent hashing
            String json = objectMapper.writeValueAsString(data);
            // Obtain a MessageDigest instance configured to use the SHA-256 algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Compute the SHA-256 hash of the JSON string's UTF-8 byte representation
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            // Convert the raw byte array hash to a human-readable hexadecimal string and return it
            return bytesToHex(hash);
        // Catch any exception that may occur during serialization or hashing (e.g., NoSuchAlgorithmException, JsonProcessingException)
        } catch (Exception e) {
            // Log an error indicating the hash calculation failed
            logger.error("Failed to calculate hash", e);
            // Fall back to a random UUID as the hash, which means this event will not be deduplicated by content
            return UUID.randomUUID().toString();
        }
    }

    // Private helper method that converts a byte array to its hexadecimal string representation
    private String bytesToHex(byte[] bytes) {
        // Create a StringBuilder to efficiently build the hex string character by character
        StringBuilder sb = new StringBuilder();
        // Iterate over each byte in the input array
        for (byte b : bytes) {
            // Format each byte as a two-character lowercase hexadecimal string and append it to the builder
            sb.append(String.format("%02x", b));
        }
        // Return the complete hexadecimal string representation of the byte array
        return sb.toString();
    }

    /**
     * Build response map
     */
    // Private helper method that constructs a response map from an existing ProducedEvent, used when returning duplicate detection results
    private Map<String, Object> buildResponse(ProducedEvent event, boolean isDuplicate) {
        // Return an immutable map containing the event details and duplicate status
        return Map.of(
            // Indicates the operation completed successfully (even for duplicates, the original was successful)
            "success", true,
            // The event ID of the originally published event
            "eventId", event.getEventId(),
            // The Kafka topic where the original event was published
            "topic", event.getTopic(),
            // The Kafka partition where the original event is stored
            "partition", event.getPartition(),
            // The Kafka offset of the original event within its partition
            "offset", event.getOffset(),
            // Boolean flag indicating whether this response represents a duplicate detection (true) or new event (false)
            "duplicate", isDuplicate,
            // The timestamp when the original event was first published, converted to a string for JSON serialization
            "originallyPublishedAt", event.getPublishedAt().toString()
        );
    }
}
