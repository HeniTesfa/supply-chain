package com.supplychain.tradeitem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.tradeitem.entity.TradeItemEntity;
import com.supplychain.tradeitem.repository.TradeItemRepository;
import com.supplychain.tradeitem.service.TradeItemProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TradeItemController.class)
class TradeItemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TradeItemProcessingService processingService;
    @MockBean private TradeItemRepository tradeItemRepository;

    // ── POST /api/process ─────────────────────────────────────────────────────

    @Test
    void processTradeItem_success_returnsOk() throws Exception {
        doNothing().when(processingService).processTradeItem(any());

        Map<String, Object> event = Map.of("gtin", "12345678901234", "skuId", "SKU001");

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(content().string("Trade item processed successfully"));
    }

    @Test
    void processTradeItem_validationFails_returns500() throws Exception {
        doThrow(new IllegalArgumentException("GTIN is required"))
                .when(processingService).processTradeItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":\"SKU001\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GTIN is required")));
    }

    @Test
    void processTradeItem_serviceFails_returns500() throws Exception {
        doThrow(new RuntimeException("Unexpected error"))
                .when(processingService).processTradeItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("gtin", "12345678"))))
                .andExpect(status().isInternalServerError());
    }

    // ── GET /api/trade-items ──────────────────────────────────────────────────

    @Test
    void getAllTradeItems_returnsTradeItemList() throws Exception {
        TradeItemEntity item = TradeItemEntity.builder()
                .id("1").gtin("12345678901234").skuId("SKU001").supplierId("SUP001").build();
        when(tradeItemRepository.findAll()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/trade-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gtin").value("12345678901234"))
                .andExpect(jsonPath("$[0].skuId").value("SKU001"));
    }

    @Test
    void getAllTradeItems_emptyList_returnsEmptyArray() throws Exception {
        when(tradeItemRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/trade-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/trade-items/{id} ─────────────────────────────────────────────

    @Test
    void getTradeItemById_found_returnsItem() throws Exception {
        TradeItemEntity item = TradeItemEntity.builder().id("1").gtin("12345678901234").skuId("SKU001").build();
        when(tradeItemRepository.findById("1")).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/trade-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtin").value("12345678901234"));
    }

    @Test
    void getTradeItemById_notFound_returns404() throws Exception {
        when(tradeItemRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/trade-items/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/trade-items/gtin/{gtin} ──────────────────────────────────────

    @Test
    void getTradeItemByGtin_found_returnsItem() throws Exception {
        TradeItemEntity item = TradeItemEntity.builder().id("1").gtin("12345678901234").skuId("SKU001").build();
        when(tradeItemRepository.findByGtin("12345678901234")).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/trade-items/gtin/12345678901234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtin").value("12345678901234"));
    }

    @Test
    void getTradeItemByGtin_notFound_returns404() throws Exception {
        when(tradeItemRepository.findByGtin("00000000000000")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/trade-items/gtin/00000000000000"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/trade-items/supplier/{supplierId} ────────────────────────────

    @Test
    void getTradeItemsBySupplierId_returnsMatchingItems() throws Exception {
        TradeItemEntity item = TradeItemEntity.builder().id("1").gtin("12345678901234").supplierId("SUP001").build();
        when(tradeItemRepository.findBySupplierId("SUP001")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/trade-items/supplier/SUP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierId").value("SUP001"));
    }

    @Test
    void getTradeItemsBySupplierId_noMatches_returnsEmptyArray() throws Exception {
        when(tradeItemRepository.findBySupplierId("SUP999")).thenReturn(List.of());

        mockMvc.perform(get("/api/trade-items/supplier/SUP999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/health ───────────────────────────────────────────────────────

    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("trade-item-service"));
    }
}
