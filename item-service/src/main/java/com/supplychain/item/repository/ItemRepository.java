// Declares this interface belongs to the item service's repository layer package
package com.supplychain.item.repository;

// Import the ItemEntity class that this repository manages and performs queries against
import com.supplychain.item.entity.ItemEntity;
// Import Spring Data MongoDB's MongoRepository interface which provides built-in CRUD operations
// (save, findById, findAll, deleteById, count, etc.) for ItemEntity documents with String-type primary keys
import org.springframework.data.mongodb.repository.MongoRepository;
// Import Spring's @Repository stereotype annotation which marks this interface as a data access component
// and enables automatic exception translation from MongoDB-specific exceptions to Spring's DataAccessException hierarchy
import org.springframework.stereotype.Repository;

// Import the List interface used as the return type for query methods that may return multiple matching documents
import java.util.List;

// @Repository marks this interface as a Spring Data repository bean, enabling component scanning
// and registering it for automatic proxy generation by Spring Data MongoDB at runtime
@Repository
// Declares the ItemRepository interface extending MongoRepository with ItemEntity as the entity type
// and String as the ID type; Spring Data MongoDB auto-implements this interface at runtime by generating
// a proxy class that translates method signatures into MongoDB queries
// This provides all standard CRUD operations plus the custom query methods defined below
public interface ItemRepository extends MongoRepository<ItemEntity, String> {
    // Spring Data derives a MongoDB query from the method name: finds all ItemEntity documents where
    // the "status" field matches the given parameter value (e.g., "ACTIVE", "INACTIVE", "DISCONTINUED")
    // Returns a List<ItemEntity> containing all matching documents, or an empty list if none are found
    // The generated MongoDB query equivalent is: db.items.find({ "status": status })
    List<ItemEntity> findByStatus(String status);
}
