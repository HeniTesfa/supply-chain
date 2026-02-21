package com.supplychain.tradeitem.entity;

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
@Document(collection = "trade_items")
public class TradeItemEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String gtin;

    private String skuId;
    private String supplierId;
    private String supplierName;
    private Integer minOrderQuantity;
    private Integer leadTimeDays;
    private String unitOfMeasure;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
