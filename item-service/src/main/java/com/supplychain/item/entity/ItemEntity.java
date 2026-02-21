// Declares this class belongs to the item service's entity layer package
package com.supplychain.item.entity;

// Import Lombok's @AllArgsConstructor which generates a constructor with parameters for all fields in the class
import lombok.AllArgsConstructor;
// Import Lombok's @Builder which implements the Builder design pattern, enabling fluent object construction
// (e.g., ItemEntity.builder().name("Laptop").price(999.99).build())
import lombok.Builder;
// Import Lombok's @Data annotation which auto-generates getters, setters, toString(), equals(), and hashCode() methods at compile time
import lombok.Data;
// Import Lombok's @NoArgsConstructor which generates a no-argument constructor required by frameworks like MongoDB for deserialization
import lombok.NoArgsConstructor;
// Import Spring Data's @Id annotation which marks a field as the primary key / document identifier in MongoDB
import org.springframework.data.annotation.Id;
// Import Spring Data MongoDB's @Document annotation which maps this Java class to a specific MongoDB collection
import org.springframework.data.mongodb.core.mapping.Document;

// Import LocalDateTime for representing timestamps without timezone information (used for createdAt and updatedAt fields)
import java.time.LocalDateTime;

// @Data is a Lombok convenience annotation that bundles @Getter, @Setter, @ToString, @EqualsAndHashCode, and @RequiredArgsConstructor
// This eliminates the need to write boilerplate getter/setter methods for each field
@Data
// @Builder enables the Builder pattern on this class, allowing construction like ItemEntity.builder().name("x").build()
// This provides a clean, fluent API for creating ItemEntity instances with only the fields you want to set
@Builder
// @NoArgsConstructor generates a public no-argument constructor, required by Spring Data MongoDB for document-to-object mapping
// Without this, MongoDB would not be able to deserialize documents into ItemEntity objects
@NoArgsConstructor
// @AllArgsConstructor generates a constructor that accepts one argument for each field in the class, in declaration order
// This works together with @Builder to support the builder pattern internally
@AllArgsConstructor
// @Document(collection = "items") maps this entity to the "items" collection in MongoDB
// When ItemEntity objects are saved via the repository, they are stored as documents in the "items" collection
@Document(collection = "items")
// Declares the ItemEntity class which represents an item document stored in the MongoDB "items" collection
// This entity models a supply chain item with basic attributes like name, description, price, and status
public class ItemEntity {
    // @Id marks this field as the MongoDB document's primary key (_id field)
    // MongoDB auto-generates a unique 24-character hex ObjectId string if this field is left null when saving
    @Id
    // The unique MongoDB document identifier for this item, auto-generated if not explicitly set
    private String id;
    // The human-readable name of the item (e.g., "Laptop", "Wireless Mouse")
    private String name;
    // A detailed description of the item providing additional information about the product
    private String description;
    // The price of the item as a Double, allowing null values for items without a set price
    // Using Double (wrapper class) instead of double (primitive) allows this field to be optional/nullable in MongoDB
    private Double price;
    // The current status of the item (e.g., "ACTIVE", "INACTIVE", "DISCONTINUED")
    // Used for filtering items by status via the ItemRepository.findByStatus() query method
    private String status;
    // The timestamp when this item document was first created in the database
    private LocalDateTime createdAt;
    // The timestamp when this item document was last updated in the database
    private LocalDateTime updatedAt;
}
