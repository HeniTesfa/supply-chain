package com.supplychain.suppliersupply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.suppliersupply.entity.SupplierSupplyEntity;
import com.supplychain.suppliersupply.repository.SupplierSupplyRepository;
import com.supplychain.suppliersupply.service.SupplierSupplyProcessingService;
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

/**
 * Controller tests for {@link SupplierSupplyController}.
 *
 * Tests the REST API endpoints using MockMvc (web layer only, no Spring context):
 * - POST /api/process - processes supplier supply events
 * - GET /api/supplier-supply - list all
 * - GET /api/supplier-supply/{id} - by ID
 * - GET /api/supplier-supply/sku/{skuId} - by SKU
 * - GET /api/supplier-supply/warehouse/{warehouseId} - by warehouse
 * - GET /api/health - health check
 */
@WebMvcTest(SupplierSupplyController.class)
class SupplierSupplyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SupplierSupplyProcessingService processingService;
    @MockBean private SupplierSupplyRepository supplierSupplyRepository;

    // ── POST /api/process ─────────────────────────────────────────────────────

    /** Verifies that a valid supplier supply event returns 200 OK. */
    @Test
    void processSupplierSupply_success_returnsOk() throws Exception {
        doNothing().when(processingService).processSupplierSupply(any());

        Map<String, Object> event = Map.of(
                "skuId", "SKU001",
                "warehouseId", "WH001",
                "availableQuantity", 100);

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(content().string("Supplier supply processed successfully"));
    }

    /** Verifies that a validation failure returns 500 with error message. */
    @Test
    void processSupplierSupply_validationFails_returns500() throws Exception {
        doThrow(new IllegalArgumentException("SKU ID is required"))
                .when(processingService).processSupplierSupply(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":\"WH001\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SKU ID is required")));
    }

    /** Verifies that a service failure returns 500 with error details. */
    @Test
    void processSupplierSupply_serviceFails_returns500() throws Exception {
        doThrow(new RuntimeException("DB error"))
                .when(processingService).processSupplierSupply(any());

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("skuId", "SKU001"))))
                .andExpect(status().isInternalServerError());
    }

    // ── GET /api/supplier-supply ──────────────────────────────────────────────

    @Test
    void getAllSupplierSupply_returnsList() throws Exception {
        SupplierSupplyEntity entity = SupplierSupplyEntity.builder()
                .id("1").skuId("SKU001").warehouseId("WH001").availableQuantity(100).build();
        when(supplierSupplyRepository.findAll()).thenReturn(List.of(entity));

        mockMvc.perform(get("/api/supplier-supply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuId").value("SKU001"))
                .andExpect(jsonPath("$[0].warehouseId").value("WH001"));
    }

    @Test
    void getAllSupplierSupply_emptyList_returnsEmptyArray() throws Exception {
        when(supplierSupplyRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/supplier-supply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/supplier-supply/{id} ─────────────────────────────────────────

    @Test
    void getSupplierSupplyById_found_returnsEntity() throws Exception {
        SupplierSupplyEntity entity = SupplierSupplyEntity.builder()
                .id("1").skuId("SKU001").warehouseId("WH001").build();
        when(supplierSupplyRepository.findById("1")).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/api/supplier-supply/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value("SKU001"));
    }

    @Test
    void getSupplierSupplyById_notFound_returns404() throws Exception {
        when(supplierSupplyRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/supplier-supply/999"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/supplier-supply/sku/{skuId} ──────────────────────────────────

    @Test
    void getSupplierSupplyBySkuId_returnsMatchingEntities() throws Exception {
        SupplierSupplyEntity entity = SupplierSupplyEntity.builder()
                .id("1").skuId("SKU001").warehouseId("WH001").availableQuantity(50).build();
        when(supplierSupplyRepository.findBySkuId("SKU001")).thenReturn(List.of(entity));

        mockMvc.perform(get("/api/supplier-supply/sku/SKU001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuId").value("SKU001"));
    }

    @Test
    void getSupplierSupplyBySkuId_noMatches_returnsEmptyArray() throws Exception {
        when(supplierSupplyRepository.findBySkuId("SKU999")).thenReturn(List.of());

        mockMvc.perform(get("/api/supplier-supply/sku/SKU999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/supplier-supply/warehouse/{warehouseId} ──────────────────────

    @Test
    void getSupplierSupplyByWarehouse_returnsMatchingEntities() throws Exception {
        SupplierSupplyEntity entity = SupplierSupplyEntity.builder()
                .id("1").skuId("SKU001").warehouseId("WH001").availableQuantity(50).build();
        when(supplierSupplyRepository.findByWarehouseId("WH001")).thenReturn(List.of(entity));

        mockMvc.perform(get("/api/supplier-supply/warehouse/WH001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseId").value("WH001"));
    }

    @Test
    void getSupplierSupplyByWarehouse_noMatches_returnsEmptyArray() throws Exception {
        when(supplierSupplyRepository.findByWarehouseId("WH999")).thenReturn(List.of());

        mockMvc.perform(get("/api/supplier-supply/warehouse/WH999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/health ───────────────────────────────────────────────────────

    /** Verifies that the health endpoint returns UP status for supplier-supply-service. */
    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("supplier-supply-service"));
    }
}
