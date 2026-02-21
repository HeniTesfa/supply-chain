package com.supplychain.consumer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * Supplier Supply Event
 * 
 * Event for supplier supply level changes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SupplierSupplyEvent extends SupplyChainEvent {

    private String supplierId;
    private String skuId;
    private String warehouseId;
    private String warehouseName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer onOrderQuantity;
    private Integer reorderPoint;
    private Integer reorderQuantity;
}
