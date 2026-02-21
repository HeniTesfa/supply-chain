package com.supplychain.consumer.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Error Log MongoDB Document
 * 
 * Tracks processing errors for monitoring and retry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "error_logs")
public class ErrorLog {
    
    @Id
    private String id;
    
    @Indexed
    private String eventId;
    
    private String eventType;      // item, trade-item, supplier-supply, shipment
    private String failureStage;    // DB_SAVE, OSP_API, LOADER_SERVICE
    private String errorMessage;
    private String stackTrace;
    private Object eventData;       // Original event data
    
    @Indexed
    private LocalDateTime failedAt;
    
    @Indexed
    private Boolean resolved;       // False = needs attention
    
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
    private Integer retryCount;
}
