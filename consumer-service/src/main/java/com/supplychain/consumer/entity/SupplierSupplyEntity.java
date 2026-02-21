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
// @Builder enables the builder pattern for constructing SupplierSupplyEntity instances fluently
@Builder
// @Document maps this class to the "supplier_supply" collection in MongoDB; each instance is one document in that collection
@Document(collection = "supplier_supply")
public class SupplierSupplyEntity {

    // @Id marks the supplierId field as the MongoDB document identifier (_id field)
    // The supplier ID serves as the natural primary key for supplier supply records
    @Id
    private String supplierId;

    // @Indexed(unique = true) creates a unique index on eventId in MongoDB,
    // ensuring no two supplier supply documents share the same event identifier; supports Level 2 deduplication
    @Indexed(unique = true)
    private String eventId;

    // The SKU (Stock Keeping Unit) identifier linking this supply record to a specific product
    private String skuId;

    // @Indexed creates a non-unique index on warehouseId for faster lookups when querying by warehouse
    // This is not unique because multiple supplier supply records can reference the same warehouse
    @Indexed
    private String warehouseId;

    // The human-readable name of the warehouse where this supply is stored (e.g., "East Coast Distribution Center")
    private String warehouseName;

    // The quantity of units currently available in stock and ready for allocation or shipment
    private Integer availableQuantity;

    // The quantity of units that have been reserved for pending orders but not yet shipped
    private Integer reservedQuantity;

    // The quantity of units that have been ordered from the supplier and are awaiting delivery
    private Integer onOrderQuantity;

    // The inventory level threshold at which a new purchase order should be triggered (minimum stock level)
    private Integer reorderPoint;

    // The number of units to order when inventory drops to or below the reorder point
    private Integer reorderQuantity;

    // The timestamp when this supplier supply document was first created in MongoDB
    private LocalDateTime createdAt;

    // The timestamp when this supplier supply document was last updated in MongoDB
    private LocalDateTime updatedAt;

    // The identifier of the source system that originated this event (e.g., "WMS", "ERP", "Inventory System")
    private String sourceSystem;

    // The current processing status of this entity, using the ProcessingStatus enum
    // Tracks the event processing lifecycle: PENDING -> PROCESSING -> SUCCESS/FAILED/DUPLICATE/SKIPPED
    private ProcessingStatus processingStatus;

    // A SHA-256 hash of the event's data content, used for Level 4 deduplication (content hash check)
    // Prevents processing duplicate events that have different IDs but identical content
    private String dataChecksum;
}
