// Declares this class belongs to the producer service's entity layer package
package com.supplychain.producer.entity;

// Import Lombok's @Data annotation which auto-generates getters, setters, toString(), equals(), and hashCode() methods at compile time
import lombok.Data;
// Import Lombok's @NoArgsConstructor which generates a no-argument constructor required by frameworks like MongoDB for deserialization
import lombok.NoArgsConstructor;
// Import Lombok's @AllArgsConstructor which generates a constructor with parameters for all fields, useful for manual object creation
import lombok.AllArgsConstructor;
// Import Lombok's @Builder which implements the Builder design pattern, enabling fluent object construction (e.g., ProducedEvent.builder().eventId("...").build())
import lombok.Builder;
// Import Spring Data's @Id annotation which marks a field as the primary key / document identifier in MongoDB
import org.springframework.data.annotation.Id;
// Import Spring Data MongoDB's @Indexed annotation which creates a MongoDB index on the annotated field for faster query performance
import org.springframework.data.mongodb.core.index.Indexed;
// Import Spring Data MongoDB's @Document annotation which maps this Java class to a specific MongoDB collection
import org.springframework.data.mongodb.core.mapping.Document;

// Import LocalDateTime for representing timestamps without timezone information (used for publishedAt field)
import java.time.LocalDateTime;

/**
 * Produced Event
 *
 * Tracks events published by producer service for deduplication
 * Prevents duplicate event creation
 */
// @Data is a Lombok convenience annotation that bundles @Getter, @Setter, @ToString, @EqualsAndHashCode, and @RequiredArgsConstructor
@Data
// @NoArgsConstructor generates a public no-argument constructor, required by Spring Data MongoDB for document-to-object mapping
@NoArgsConstructor
// @AllArgsConstructor generates a constructor that accepts one argument for each field in the class, in declaration order
@AllArgsConstructor
// @Builder enables the Builder pattern on this class, allowing construction like ProducedEvent.builder().eventId("x").topic("y").build()
@Builder
// @Document(collection = "produced_events") maps this entity to the "produced_events" collection in MongoDB
// When this object is saved via the repository, it will be stored as a document in the "produced_events" collection
@Document(collection = "produced_events")
// Declares the ProducedEvent class which represents a record of an event that was published to Kafka,
// stored in MongoDB for producer-side deduplication and audit trail purposes
public class ProducedEvent {

    // @Id marks this field as the MongoDB document's primary key (_id field); MongoDB auto-generates an ObjectId if not set
    @Id
    // The unique MongoDB document identifier, auto-generated as a 24-character hex string if left null
    private String id;

    // @Indexed(unique = true) creates a unique index on eventId in MongoDB, ensuring no two documents can have the same eventId
    // This prevents accidental insertion of duplicate events and speeds up lookups by eventId
    @Indexed(unique = true)
    // The application-generated unique event identifier (e.g., "evt_item_a1b2c3d4"), used as the Kafka message key
    private String eventId;           // Unique event ID

    // @Indexed creates a non-unique index on idempotencyKey in MongoDB, enabling fast lookups for deduplication checks
    // When a client sends the same Idempotency-Key header, the system can quickly find the original event
    @Indexed
    // The client-provided idempotency key from the HTTP request header; may be null if the client did not provide one
    private String idempotencyKey;    // Client-provided idempotency key

    // @Indexed creates a non-unique index on contentHash in MongoDB, enabling fast lookups for content-based deduplication
    // If two events have the same SHA-256 content hash, they contain identical data
    @Indexed
    // The SHA-256 hash of the serialized event data payload, used to detect duplicate content even without idempotency keys
    private String contentHash;       // Hash of event content (for duplicate detection)

    // The type of supply chain event: "item", "trade-item", "supplier-supply", or "shipment"
    private String eventType;         // item, trade-item, supplier-supply, shipment
    // The Kafka topic name where this event was published (e.g., "supply-chain.item.events")
    private String topic;             // Kafka topic where event was published
    // The Kafka partition number within the topic where this specific message was stored
    private Integer partition;        // Kafka partition
    // The Kafka offset position of this message within its partition, representing its sequential order
    private Long offset;              // Kafka offset

    // @Indexed creates an index on publishedAt for efficient time-range queries (e.g., finding events published in a given time window)
    @Indexed
    // The timestamp when this event was successfully published to Kafka, recorded as a LocalDateTime
    private LocalDateTime publishedAt;

    // The original event payload data stored as a generic Object; MongoDB stores this as an embedded document
    // This preserves the full event data for audit trail and potential replay scenarios
    private Object eventData;         // Original event payload
    // The identifier of the system that produced this event (always "producer-service" in this application)
    private String sourceSystem;
}
