// Package declaration: this class belongs to the entity sub-package of the consumer service
package com.supplychain.consumer.entity;

// Import Lombok's @Data annotation, which auto-generates getters, setters, toString(), equals(), and hashCode() methods at compile time
import lombok.Data;

// Import Lombok's @NoArgsConstructor annotation, which generates a no-argument (default) constructor
import lombok.NoArgsConstructor;

// Import Lombok's @AllArgsConstructor annotation, which generates a constructor with parameters for all fields
import lombok.AllArgsConstructor;

// Import Lombok's @Builder annotation, which implements the Builder design pattern for fluent object construction
import lombok.Builder;

// Import Spring Data's @Id annotation, which marks a field as the primary key (document ID) in MongoDB
import org.springframework.data.annotation.Id;

// Import Spring Data MongoDB's @Indexed annotation, which creates an index on the annotated field for faster queries
import org.springframework.data.mongodb.core.index.Indexed;

// Import Spring Data MongoDB's @Document annotation, which maps this class to a specific MongoDB collection
import org.springframework.data.mongodb.core.mapping.Document;

// Import LocalDateTime from java.time, used for storing creation and update timestamps
import java.time.LocalDateTime;

// @Data generates getters, setters, toString(), equals(), and hashCode() via Lombok to reduce boilerplate
@Data
// @NoArgsConstructor generates a public no-argument constructor, required by MongoDB for document deserialization
@NoArgsConstructor
// @AllArgsConstructor generates a constructor accepting one argument for every field in the class
@AllArgsConstructor
// @Builder enables the builder pattern for constructing TradeItemEntity instances fluently
@Builder
// @Document maps this class to the "trade_items" collection in MongoDB; each instance is one document in that collection
@Document(collection = "trade_items")
public class TradeItemEntity {

    // @Id marks the gtin field as the MongoDB document identifier (_id field)
    // The GTIN (Global Trade Item Number) serves as the natural primary key for trade items
    @Id
    private String gtin;

    // @Indexed(unique = true) creates a unique index on eventId in MongoDB,
    // ensuring no two trade item documents share the same event identifier; supports Level 2 deduplication
    @Indexed(unique = true)
    private String eventId;

    // The SKU (Stock Keeping Unit) identifier linking this trade item to its corresponding item entity
    private String skuId;

    // The unique identifier of the supplier who provides this trade item
    private String supplierId;

    // The human-readable name of the supplier (e.g., "Acme Corp")
    private String supplierName;

    // A textual description of the trade item, providing details about the product
    private String description;

    // The unit of measure for ordering and inventory (e.g., "EACH", "CASE", "PALLET")
    private String unitOfMeasure;

    // The minimum number of units that must be ordered from the supplier at one time
    private Integer minOrderQuantity;

    // The number of days the supplier needs to fulfill an order from the time it is placed
    private Integer leadTimeDays;

    // The timestamp when this trade item document was first created in MongoDB
    private LocalDateTime createdAt;

    // The timestamp when this trade item document was last updated in MongoDB
    private LocalDateTime updatedAt;

    // The identifier of the source system that originated this event (e.g., "ERP", "PIM")
    private String sourceSystem;

    // The current processing status of this entity, using the ProcessingStatus enum
    // Tracks the event processing lifecycle: PENDING -> PROCESSING -> SUCCESS/FAILED/DUPLICATE/SKIPPED
    private ProcessingStatus processingStatus;

    // A SHA-256 hash of the event's data content, used for Level 4 deduplication (content hash check)
    // Prevents processing duplicate events that have different IDs but identical content
    private String dataChecksum;
}
