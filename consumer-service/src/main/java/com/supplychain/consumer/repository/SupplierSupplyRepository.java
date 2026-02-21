package com.supplychain.consumer.repository;

import com.supplychain.consumer.entity.ProcessingStatus;
import com.supplychain.consumer.entity.SupplierSupplyEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Supplier Supply Repository
 * 
 * Provides CRUD operations for Supplier Supply entities
 */
@Repository
public interface SupplierSupplyRepository extends MongoRepository<SupplierSupplyEntity, String> {
    
    Optional<SupplierSupplyEntity> findByEventId(String eventId);
    
    // Composite business key: Supplier Id
    Optional<SupplierSupplyEntity> findBySupplierId(String supplierId);
    
    List<SupplierSupplyEntity> findByProcessingStatus(ProcessingStatus status);
}
