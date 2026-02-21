package com.supplychain.consumer.repository;

import com.supplychain.consumer.entity.TradeItemEntity;
import com.supplychain.consumer.entity.ProcessingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Trade Item Repository
 * 
 * Provides CRUD operations for Trade Item entities
 */
@Repository
public interface TradeItemRepository extends MongoRepository<TradeItemEntity, String> {
    
    Optional<TradeItemEntity> findByEventId(String eventId);
    Optional<TradeItemEntity> findByGtin(String gtin);
    List<TradeItemEntity> findByProcessingStatus(ProcessingStatus status);
}
