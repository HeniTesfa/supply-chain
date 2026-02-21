package com.supplychain.shipment.repository;

import com.supplychain.shipment.entity.ShipmentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends MongoRepository<ShipmentEntity, String> {
    Optional<ShipmentEntity> findByTrackingNumber(String trackingNumber);
    List<ShipmentEntity> findByShipmentStatus(String shipmentStatus);
    List<ShipmentEntity> findByOrderId(String orderId);
}
