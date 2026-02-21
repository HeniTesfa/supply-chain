// Declares this interface belongs to the producer service's repository layer package
package com.supplychain.producer.repository;

// Import the ProducedEvent entity class that this repository manages and performs queries against
import com.supplychain.producer.entity.ProducedEvent;
// Import Spring Data MongoDB's MongoRepository interface which provides CRUD operations (save, findById, findAll, delete, etc.)
// MongoRepository<ProducedEvent, String> means this repository manages ProducedEvent documents with String-type primary keys
import org.springframework.data.mongodb.repository.MongoRepository;
// Import Spring's @Repository stereotype annotation which marks this interface as a data access component
// and enables automatic exception translation from MongoDB-specific exceptions to Spring's DataAccessException hierarchy
import org.springframework.stereotype.Repository;

// Import Optional which is a container that may or may not hold a non-null value, used to safely represent query results
// that may return zero or one matching document
import java.util.Optional;

/**
 * Produced Event Repository
 *
 * Data access for tracking published events
 */
// @Repository marks this interface as a Spring Data repository bean, enabling component scanning and exception translation
@Repository
// Declares the ProducedEventRepository interface extending MongoRepository with ProducedEvent as the entity type
// and String as the ID type; Spring Data MongoDB auto-implements this interface at runtime by generating a proxy class
// that translates the method signatures into MongoDB queries
public interface ProducedEventRepository extends MongoRepository<ProducedEvent, String> {

    /**
     * Find by idempotency key (prevents duplicate submissions)
     */
    // Spring Data derives a MongoDB query from the method name: finds a single ProducedEvent document where
    // the "idempotencyKey" field matches the given parameter; returns Optional.empty() if no match is found
    // This is the first deduplication check in the producer service - it prevents clients from submitting the same request twice
    Optional<ProducedEvent> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find by content hash (detects duplicate content)
     */
    // Spring Data derives a MongoDB query from the method name: finds a single ProducedEvent document where
    // the "contentHash" field matches the given SHA-256 hash; returns Optional.empty() if no match is found
    // This is the second deduplication check - it detects identical event payloads even if different idempotency keys are used
    Optional<ProducedEvent> findByContentHash(String contentHash);

    /**
     * Find by event ID
     */
    // Spring Data derives a MongoDB query from the method name: finds a single ProducedEvent document where
    // the "eventId" field matches the given event identifier; returns Optional.empty() if no match is found
    // This enables looking up events by their unique application-generated event ID (e.g., "evt_item_a1b2c3d4")
    Optional<ProducedEvent> findByEventId(String eventId);
}
