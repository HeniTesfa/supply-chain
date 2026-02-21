package com.supplychain.shipment.service;

import com.supplychain.shipment.entity.ShipmentEntity;
import com.supplychain.shipment.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ShipmentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentProcessingService.class);

    @Autowired
    private ShipmentRepository shipmentRepository;

    public void processShipment(Map<String, Object> shipmentEvent) {
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");
        String carrier = (String) shipmentEvent.get("carrier");
        String shipmentStatus = (String) shipmentEvent.get("shipmentStatus");

        logger.info("Processing shipment - Tracking: {}, Carrier: {}, Status: {}", trackingNumber, carrier, shipmentStatus);

        validateShipment(shipmentEvent);
        saveShipment(shipmentEvent);
        processShipmentStatus(shipmentStatus, shipmentEvent);

        logger.info("Shipment processing completed successfully");
    }

    private void saveShipment(Map<String, Object> shipmentEvent) {
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");

        ShipmentEntity entity = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElse(ShipmentEntity.builder()
                        .trackingNumber(trackingNumber)
                        .createdAt(LocalDateTime.now())
                        .build());

        entity.setOrderId((String) shipmentEvent.get("orderId"));
        entity.setCarrier((String) shipmentEvent.get("carrier"));
        entity.setShipmentStatus((String) shipmentEvent.get("shipmentStatus"));
        entity.setCurrentLocation((String) shipmentEvent.get("currentLocation"));
        entity.setEstimatedDelivery((String) shipmentEvent.get("estimatedDelivery"));
        entity.setUpdatedAt(LocalDateTime.now());

        shipmentRepository.save(entity);
        logger.info("Shipment saved to MongoDB - Tracking: {}", trackingNumber);
    }

    private void validateShipment(Map<String, Object> shipmentEvent) {
        if (!shipmentEvent.containsKey("trackingNumber")) {
            throw new IllegalArgumentException("Tracking number is required");
        }
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Tracking number cannot be empty");
        }
        if (!shipmentEvent.containsKey("carrier")) {
            throw new IllegalArgumentException("Carrier is required");
        }
        String carrier = (String) shipmentEvent.get("carrier");
        if (carrier == null || carrier.trim().isEmpty()) {
            throw new IllegalArgumentException("Carrier cannot be empty");
        }
    }

    private void processShipmentStatus(String status, Map<String, Object> shipmentEvent) {
        if (status == null) return;
        String trackingNumber = (String) shipmentEvent.get("trackingNumber");
        switch (status.toUpperCase()) {
            case "CREATED":
            case "LABEL_CREATED":
                logger.info("Shipment created - Tracking: {}", trackingNumber); break;
            case "IN_TRANSIT":
                logger.info("Shipment in transit - Tracking: {}", trackingNumber); break;
            case "DELIVERED":
                logger.info("Shipment delivered - Tracking: {}", trackingNumber); break;
            case "DELAYED":
                logger.warn("Shipment delayed - Tracking: {}", trackingNumber); break;
            case "EXCEPTION":
            case "FAILED_DELIVERY":
                logger.error("Shipment exception - Tracking: {}", trackingNumber); break;
            default:
                logger.debug("Shipment status: {}", status);
        }
    }
}
