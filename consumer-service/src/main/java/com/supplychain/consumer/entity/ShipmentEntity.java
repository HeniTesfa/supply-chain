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

// Import LocalDateTime from java.time, used for timestamps such as creation time, delivery dates, and update time
import java.time.LocalDateTime;

// @Data generates getters, setters, toString(), equals(), and hashCode() via Lombok to reduce boilerplate
@Data
// @NoArgsConstructor generates a public no-argument constructor, required by MongoDB for document deserialization
@NoArgsConstructor
// @AllArgsConstructor generates a constructor accepting one argument for every field in the class
@AllArgsConstructor
// @Builder enables the builder pattern for constructing ShipmentEntity instances fluently
@Builder
// @Document maps this class to the "shipments" collection in MongoDB; each instance is one document in that collection
@Document(collection = "shipments")
public class ShipmentEntity {

    // @Id marks this field as the MongoDB document identifier (_id field); MongoDB auto-generates a value if not set
    @Id
    private String id;

    // @Indexed(unique = true) creates a unique index on eventId in MongoDB,
    // ensuring no two shipment documents share the same event identifier; supports Level 2 deduplication
    @Indexed(unique = true)
    private String eventId;

    // @Indexed(unique = true) creates a unique index on trackingNumber in MongoDB,
    // ensuring each shipment has a unique tracking number; supports Level 3 deduplication (Business Key check)
    @Indexed(unique = true)
    private String trackingNumber;

    // The order identifier that this shipment is fulfilling (links the shipment to a purchase/sales order)
    private String orderId;

    // The name of the shipping carrier handling this shipment (e.g., "FedEx", "UPS", "DHL")
    private String carrier;

    // The current status of the shipment (e.g., "IN_TRANSIT", "DELIVERED", "PENDING", "RETURNED")
    private String shipmentStatus;

    // The location where the shipment originates from (e.g., warehouse address or city name)
    private String originLocation;

    // The final delivery destination for the shipment (e.g., customer address or distribution center)
    private String destinationLocation;

    // The estimated date and time when the shipment is expected to arrive at its destination
    private LocalDateTime estimatedDeliveryDate;

    // The actual date and time when the shipment was delivered; null if not yet delivered
    private LocalDateTime actualDeliveryDate;

    // The current geographical location of the shipment during transit (updated as the shipment moves)
    private String currentLocation;

    // The timestamp when this shipment document was first created in MongoDB
    private LocalDateTime createdAt;

    // The timestamp when this shipment document was last updated in MongoDB
    private LocalDateTime updatedAt;

    // The identifier of the source system that originated this event (e.g., "TMS", "WMS", "Carrier API")
    private String sourceSystem;

    // The current processing status of this entity, using the ProcessingStatus enum
    // Tracks the event processing lifecycle: PENDING -> PROCESSING -> SUCCESS/FAILED/DUPLICATE/SKIPPED
    private ProcessingStatus processingStatus;

    // A SHA-256 hash of the event's data content, used for Level 4 deduplication (content hash check)
    // Prevents processing duplicate events that have different IDs but identical content
    private String dataChecksum;
}
