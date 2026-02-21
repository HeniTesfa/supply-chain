package com.supplychain.consumer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * Trade Item Event
 * 
 * Event for GTIN and supplier information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TradeItemEvent extends SupplyChainEvent {
    
    private String gtin;
    private String skuId;
    private String supplierId;
    private String supplierName;
    private String description;
    private String unitOfMeasure;
    private Integer minOrderQuantity;
    private Integer leadTimeDays;
}
