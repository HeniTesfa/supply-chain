package com.supplychain.consumer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Shipment Event
 * 
 * Event for shipment tracking updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ShipmentEvent extends SupplyChainEvent {
    
    private String trackingNumber;
    private String orderId;
    private String carrier;
    private String shipmentStatus;
    private String originLocation;
    private String destinationLocation;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String currentLocation;
}
