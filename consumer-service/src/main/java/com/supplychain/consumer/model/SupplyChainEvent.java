package com.supplychain.consumer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base Supply Chain Event
 * 
 * Parent class for all event types
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyChainEvent {
    
    private String eventId;         // Unique event ID (for Level 2 dedup)
    private String eventType;       // item, trade-item, supplier-supply, shipment
    private String action;          // CREATE, UPDATE, DELETE
    private String sourceSystem;    // System that generated the event
    private LocalDateTime timestamp;
}
