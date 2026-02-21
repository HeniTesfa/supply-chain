package com.supplychain.consumer.repository;

import com.supplychain.consumer.entity.ItemEntity;
import com.supplychain.consumer.entity.ProcessingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Item Repository
 * 
 * Provides CRUD operations for Item entities
 */
@Repository
public interface ItemRepository extends MongoRepository<ItemEntity, String> {
    
    // Level 2 Dedup: Find by Event ID
    Optional<ItemEntity> findByEventId(String eventId);
    
    // Level 3 Dedup: Find by Business Key (SKU)
    Optional<ItemEntity> findBySkuId(String skuId);
    
    // Find by processing status (for retry/monitoring)
    List<ItemEntity> findByProcessingStatus(ProcessingStatus status);
}
