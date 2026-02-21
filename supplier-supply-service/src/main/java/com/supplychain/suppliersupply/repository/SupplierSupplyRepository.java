package com.supplychain.suppliersupply.repository;

import com.supplychain.suppliersupply.entity.SupplierSupplyEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierSupplyRepository extends MongoRepository<SupplierSupplyEntity, String> {
    List<SupplierSupplyEntity> findBySkuId(String skuId);
    List<SupplierSupplyEntity> findByWarehouseId(String warehouseId);
}
