package com.supplychain.shipment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shipments")
public class ShipmentEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String trackingNumber;

    private String orderId;
    private String carrier;
    private String shipmentStatus;
    private String currentLocation;
    private String estimatedDelivery;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
