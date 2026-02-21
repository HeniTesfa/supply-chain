package com.supplychain.consumer.repository;

import com.supplychain.consumer.entity.ShipmentEntity;
import com.supplychain.consumer.entity.ProcessingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Shipment Repository
 * 
 * Provides CRUD operations for Shipment entities
 */
@Repository
public interface ShipmentRepository extends MongoRepository<ShipmentEntity, String> {
    
    Optional<ShipmentEntity> findByEventId(String eventId);
    Optional<ShipmentEntity> findByTrackingNumber(String trackingNumber);
    List<ShipmentEntity> findByProcessingStatus(ProcessingStatus status);
}
