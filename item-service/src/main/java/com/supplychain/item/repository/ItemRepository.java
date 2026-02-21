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
import java.util.Optional;

@Repository
public interface ItemRepository extends MongoRepository<ItemEntity, String> {
    Optional<ItemEntity> findBySkuId(String skuId);
    List<ItemEntity> findByStatus(String status);
}
