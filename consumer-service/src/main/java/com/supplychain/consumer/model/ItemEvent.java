package com.supplychain.consumer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * Item Event
 * 
 * Event for item master data changes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ItemEvent extends SupplyChainEvent {
    
    private String skuId;
    private String itemName;
    private String category;
    private Double price;
    private Double weight;
    private String dimensions;
    private String status;
}
