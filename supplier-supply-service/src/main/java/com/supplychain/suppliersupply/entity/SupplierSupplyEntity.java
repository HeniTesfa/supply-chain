package com.supplychain.suppliersupply.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "supplier_supply")
public class SupplierSupplyEntity {

    @Id
    private String id;

    private String skuId;
    private String warehouseId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer onOrderQuantity;
    private Integer reorderPoint;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
