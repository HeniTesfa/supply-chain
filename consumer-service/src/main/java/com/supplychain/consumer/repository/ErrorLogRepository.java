package com.supplychain.consumer.repository;

import com.supplychain.consumer.entity.ErrorLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Error Log Repository
 * 
 * Provides CRUD operations for Error Log entities
 */
@Repository
public interface ErrorLogRepository extends MongoRepository<ErrorLog, String> {
    
    // Find unresolved errors
    List<ErrorLog> findByResolvedFalse();
    
    // Find errors by stage
    List<ErrorLog> findByFailureStageAndResolvedFalse(String failureStage);
    
    // Find errors by event type
    List<ErrorLog> findByEventTypeAndResolvedFalse(String eventType);
}
