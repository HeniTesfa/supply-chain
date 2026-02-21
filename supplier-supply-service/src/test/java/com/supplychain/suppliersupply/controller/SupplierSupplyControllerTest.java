package com.supplychain.suppliersupply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supplychain.suppliersupply.service.SupplierSupplyProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link SupplierSupplyController}.
 *
 * Tests the REST API endpoints using MockMvc (web layer only, no Spring context):
 * - POST /api/process - processes supplier supply events
 * - GET /api/health - health check
 * - Error handling when service throws exceptions
 */
@WebMvcTest(SupplierSupplyController.class)
class SupplierSupplyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SupplierSupplyProcessingService processingService;

    /** Verifies that a valid supplier supply event returns 200 OK. */
    @Test
    void processSupplierSupply_success_returnsOk() throws Exception {
        doNothing().when(processingService).processSupplierSupply(any());

        Map<String, Object> event = new HashMap<>();
        event.put("skuId", "SKU001");
        event.put("warehouseId", "WH001");
        event.put("availableQuantity", 100);

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

    /** Verifies that the health endpoint returns UP status for supplier-supply-service. */
    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("supplier-supply-service"));
    }
}
