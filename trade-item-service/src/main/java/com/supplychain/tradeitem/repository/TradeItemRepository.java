package com.supplychain.tradeitem.repository;

import com.supplychain.tradeitem.entity.TradeItemEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeItemRepository extends MongoRepository<TradeItemEntity, String> {
    Optional<TradeItemEntity> findByGtin(String gtin);
    List<TradeItemEntity> findBySkuId(String skuId);
    List<TradeItemEntity> findBySupplierId(String supplierId);
}
