// Package declaration: this class belongs to the entity sub-package of the consumer service
package com.supplychain.consumer.entity;

// Import Lombok's @Data annotation, which auto-generates getters, setters, toString(), equals(), and hashCode() methods at compile time
import lombok.Data;

// Import Lombok's @NoArgsConstructor annotation, which generates a no-argument (default) constructor
import lombok.NoArgsConstructor;

// Import Lombok's @AllArgsConstructor annotation, which generates a constructor with parameters for all fields
import lombok.AllArgsConstructor;

// Import Lombok's @Builder annotation, which implements the Builder design pattern for creating instances of this class
import lombok.Builder;

// Import Spring Data's @Id annotation, which marks a field as the primary key (document ID) in MongoDB
import org.springframework.data.annotation.Id;

// Import Spring Data MongoDB's @Indexed annotation, which creates an index on the annotated field in MongoDB for faster queries
import org.springframework.data.mongodb.core.index.Indexed;

// Import Spring Data MongoDB's @Document annotation, which maps this class to a specific MongoDB collection
import org.springframework.data.mongodb.core.mapping.Document;

// Import LocalDateTime from the java.time package, used for storing timestamps without timezone information
import java.time.LocalDateTime;

// @Data is a Lombok annotation that generates getters for all fields, setters for all non-final fields,
// toString(), equals(), hashCode(), and a required-args constructor, reducing boilerplate code significantly
@Data
// @NoArgsConstructor generates a public constructor with no parameters, required by MongoDB for deserialization
@NoArgsConstructor
// @AllArgsConstructor generates a constructor that accepts one argument for each field in the class
@AllArgsConstructor
// @Builder enables the builder pattern (e.g., ItemEntity.builder().skuId("SKU123").build()) for fluent object construction
@Builder
// @Document maps this Java class to the "items" collection in MongoDB; each instance represents one document in that collection
@Document(collection = "items")
public class ItemEntity {

    // @Id marks this field as the MongoDB document identifier (_id field); MongoDB auto-generates a value if not set
    @Id
    private String id;

    // @Indexed(unique = true) creates a unique index on the eventId field in MongoDB,
    // ensuring no two item documents can have the same eventId; this supports Level 2 deduplication (Event ID check)
    @Indexed(unique = true)
    private String eventId;

    // @Indexed(unique = true) creates a unique index on the skuId field in MongoDB,
    // ensuring no two item documents share the same SKU identifier; this supports Level 3 deduplication (Business Key check)
    @Indexed(unique = true)
    private String skuId;

    // The human-readable name of the item (e.g., "Industrial Valve 3-inch")
    private String itemName;

    // The category classification for the item (e.g., "Electronics", "Raw Materials", "Packaging")
    private String category;

    // The unit price of the item, stored as a Double to support decimal values (e.g., 29.99)
    private Double price;

    // The weight of the item, stored as a Double to support decimal measurements (e.g., 2.5 kg)
    private Double weight;

    // The physical dimensions of the item as a formatted string (e.g., "10x5x3 cm")
    private String dimensions;

    // The current status of the item (e.g., "ACTIVE", "DISCONTINUED", "PENDING")
    private String status;

    // The timestamp when this item document was first created in MongoDB
    private LocalDateTime createdAt;

    // The timestamp when this item document was last updated in MongoDB
    private LocalDateTime updatedAt;

    // The identifier of the system that originated this event (e.g., "ERP", "WMS", "Manual Entry")
    private String sourceSystem;

    // The current processing status of this entity, using the ProcessingStatus enum
    // Tracks the lifecycle of event processing: PENDING -> PROCESSING -> SUCCESS/FAILED/DUPLICATE/SKIPPED
    private ProcessingStatus processingStatus;

    // A SHA-256 hash of the event's content, used for Level 4 deduplication (content hash check)
    // If two events produce the same checksum, the second is treated as a duplicate even if IDs differ
    private String dataChecksum;
}
