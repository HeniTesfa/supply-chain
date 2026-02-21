package com.supplychain.consumer.entity;

/**
 * Processing Status Enum
 * 
 * Tracks the processing state of events
 */
public enum ProcessingStatus {
    PENDING,      // Event received, not processed yet
    PROCESSING,   // Currently being processed
    SUCCESS,      // Successfully processed
    FAILED,       // Processing failed
    DUPLICATE,    // Duplicate event detected
    SKIPPED       // Skipped (no changes detected)
}
