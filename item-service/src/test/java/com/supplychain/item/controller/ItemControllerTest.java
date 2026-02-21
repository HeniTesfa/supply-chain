package com.supplychain.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.item.entity.ItemEntity;
import com.supplychain.item.repository.ItemRepository;
import com.supplychain.item.service.ItemProcessingService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ItemProcessingService processingService;
    @MockBean private ItemRepository itemRepository;

    // ── POST /api/process ─────────────────────────────────────────────────────

    @Test
    void processItem_success_returnsOk() throws Exception {
        doNothing().when(processingService).processItem(any());

        Map<String, Object> event = Map.of("skuId", "SKU001", "itemName", "Laptop");

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(content().string("Item processed successfully"));
    }

    @Test
    void processItem_validationFails_returns500() throws Exception {
        doThrow(new IllegalArgumentException("SKU ID is required"))
                .when(processingService).processItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemName\":\"Laptop\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SKU ID is required")));
    }

    @Test
    void processItem_serviceFails_returns500() throws Exception {
        doThrow(new RuntimeException("OSP API down"))
                .when(processingService).processItem(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("skuId", "SKU001"))))
                .andExpect(status().isInternalServerError());
    }

    // ── GET /api/items ────────────────────────────────────────────────────────

    @Test
    void getAllItems_returnsItemList() throws Exception {
        ItemEntity item = ItemEntity.builder()
                .id("1").skuId("SKU001").itemName("Laptop").status("ACTIVE").build();
        when(itemRepository.findAll()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuId").value("SKU001"))
                .andExpect(jsonPath("$[0].itemName").value("Laptop"));
    }

    @Test
    void getAllItems_emptyList_returnsEmptyArray() throws Exception {
        when(itemRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/items/{id} ───────────────────────────────────────────────────

    @Test
    void getItemById_found_returnsItem() throws Exception {
        ItemEntity item = ItemEntity.builder().id("1").skuId("SKU001").itemName("Laptop").build();
        when(itemRepository.findById("1")).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value("SKU001"));
    }

    @Test
    void getItemById_notFound_returns404() throws Exception {
        when(itemRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/items/sku/{skuId} ────────────────────────────────────────────

    @Test
    void getItemBySkuId_found_returnsItem() throws Exception {
        ItemEntity item = ItemEntity.builder().id("1").skuId("SKU001").itemName("Laptop").build();
        when(itemRepository.findBySkuId("SKU001")).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/sku/SKU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value("SKU001"));
    }

    @Test
    void getItemBySkuId_notFound_returns404() throws Exception {
        when(itemRepository.findBySkuId("SKU999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/sku/SKU999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/items/status/{status} ────────────────────────────────────────

    @Test
    void getItemsByStatus_returnsMatchingItems() throws Exception {
        ItemEntity item = ItemEntity.builder().id("1").skuId("SKU001").status("ACTIVE").build();
        when(itemRepository.findByStatus("ACTIVE")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/items/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getItemsByStatus_noMatches_returnsEmptyArray() throws Exception {
        when(itemRepository.findByStatus("DISCONTINUED")).thenReturn(List.of());

        mockMvc.perform(get("/api/items/status/DISCONTINUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/health ───────────────────────────────────────────────────────

    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("item-service"));
    }
}
